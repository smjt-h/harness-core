/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.managerclient;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.network.FibonacciBackOff;
import io.harness.network.Http;
import io.harness.network.NoopHostnameVerifier;
import io.harness.security.TokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.version.VersionInfoManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request.Builder;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.StringUtil;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateAgentManagerClientFactory
    implements Provider<io.harness.managerclient.DelegateAgentManagerClient> {
  public static final ImmutableList<TrustManager> TRUST_ALL_CERTS =
      ImmutableList.of(new io.harness.managerclient.DelegateAgentManagerClientX509TrustManager());

  @Inject private VersionInfoManager versionInfoManager;
  @Inject private KryoConverterFactory kryoConverterFactory;

  private String baseUrl;
  private TokenGenerator tokenGenerator;
  private String clientCertificateFilePath;
  private String clientCertificateKeyFilePath;

  DelegateAgentManagerClientFactory(String baseUrl, TokenGenerator tokenGenerator, String clientCertificateFilePath,
      String clientCertificateKeyFilePath) {
    this.baseUrl = baseUrl;
    this.tokenGenerator = tokenGenerator;
    this.clientCertificateFilePath = clientCertificateFilePath;
    this.clientCertificateKeyFilePath = clientCertificateKeyFilePath;
  }

  @Override
  public io.harness.managerclient.DelegateAgentManagerClient get() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new GuavaModule());
    objectMapper.registerModule(new JavaTimeModule());
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(getSafeOkHttpClient())
                            .addConverterFactory(kryoConverterFactory)
                            .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                            .build();
    return retrofit.create(io.harness.managerclient.DelegateAgentManagerClient.class);
  }

  private OkHttpClient getSafeOkHttpClient() {
    try {
      // Create Trust Manager (CA Certs used to verify server cert)
      KeyStore trustKeyStore = this.getTrustKeyStore();
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustKeyStore);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

      // Create Key Manager (Client Cert used for mtls) - only if client certificate provided
      KeyManager[] keyManagers = null;
      if (StringUtils.isNotEmpty(this.clientCertificateFilePath)
          && StringUtils.isNotEmpty(this.clientCertificateKeyFilePath)) {
        KeyStore clientKeyStore = this.getClientKeyStore();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(clientKeyStore, "somePassword42".toCharArray());
        keyManagers = keyManagerFactory.getKeyManagers();
      }

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, null);

      return Http.getOkHttpClientWithProxyAuthSetup()
          .hostnameVerifier(new NoopHostnameVerifier())
          .connectionPool(Http.connectionPool)
          .retryOnConnectionFailure(true)
          .addInterceptor(new io.harness.managerclient.DelegateAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustManagers[0])
          .addInterceptor(chain -> {
            Builder request = chain.request().newBuilder().addHeader(
                "User-Agent", "delegate/" + versionInfoManager.getVersionInfo().getVersion());
            return chain.proceed(request.build());
          })
          .addInterceptor(chain -> FibonacciBackOff.executeForEver(() -> chain.proceed(chain.request())))
          // During this call we not just query the task, but we also obtain the secret on the manager side
          // we need to give enough time for the call to finish.
          .readTimeout(2, TimeUnit.MINUTES)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private KeyStore getTrustKeyStore()
      throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);

    // Load self-signed certificate created only for the purpose of local development
    try (InputStream certInputStream = getClass().getClassLoader().getResourceAsStream("localhost.pem")) {
      keyStore.setCertificateEntry(
          "localhost", (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(certInputStream));
    }

    // Load all trusted issuers from default java trust store
    TrustManagerFactory defaultTrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    defaultTrustManagerFactory.init((KeyStore) null);
    for (TrustManager trustManager : defaultTrustManagerFactory.getTrustManagers()) {
      if (trustManager instanceof X509TrustManager) {
        for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
          keyStore.setCertificateEntry(acceptedIssuer.getSubjectDN().getName(), acceptedIssuer);
        }
      }
    }

    return keyStore;
  }

  private KeyStore getClientKeyStore()
      throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
    // Load Private Key
    String privateKeyContent =
        new String(Files.readAllBytes(Paths.get(this.clientCertificateKeyFilePath)), Charset.defaultCharset())
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll(System.lineSeparator(), "")
            .replace("-----END PRIVATE KEY-----", "");

    byte[] privateKeyAsBytes = Base64.getDecoder().decode(privateKeyContent);
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyAsBytes);

    // Load Certificate (For PROD readiness should be able to read chain ...)
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    InputStream certificateChainAsInputStream =
        Files.newInputStream(Paths.get(this.clientCertificateFilePath), StandardOpenOption.READ);
    Certificate certificate = certificateFactory.generateCertificate(certificateChainAsInputStream);

    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    keyStore.load(null, null);
    keyStore.setKeyEntry(
        "client", keyFactory.generatePrivate(keySpec), "somePassword42".toCharArray(), new Certificate[] {certificate});

    return keyStore;
  }

  /**
   * Let's keep this method for now, since we might want to give the options to customers to keep the unsafe way of
   * ignoring all certificate issues
   * @return
   */
  private OkHttpClient getUnsafeOkHttpClient() {
    try {
      // Install the all-trusting trust manager
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS.toArray(new TrustManager[1]), new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      return Http.getOkHttpClientWithProxyAuthSetup()
          .connectionPool(new ConnectionPool())
          .retryOnConnectionFailure(true)
          .addInterceptor(new io.harness.managerclient.DelegateAuthInterceptor(tokenGenerator))
          .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS.get(0))
          .addInterceptor(chain -> {
            Builder request = chain.request().newBuilder().addHeader(
                "User-Agent", "delegate/" + versionInfoManager.getVersionInfo().getVersion());
            return chain.proceed(request.build());
          })
          .addInterceptor(chain -> FibonacciBackOff.executeForEver(() -> chain.proceed(chain.request())))
          .hostnameVerifier(new NoopHostnameVerifier())
          // During this call we not just query the task but we also obtain the secret on the manager side
          // we need to give enough time for the call to finish.
          .readTimeout(2, TimeUnit.MINUTES)
          .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
