/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.k8s;

import static io.harness.data.encoding.EncodingUtils.encodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.filesystem.FileIo.getHomeDir;
import static io.harness.govern.Switch.noop;
import static io.harness.k8s.KubernetesConvention.DASH;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.network.Http.joinHostPort;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static io.fabric8.kubernetes.client.Config.KUBERNETES_KUBECONFIG_FILE;
import static io.fabric8.kubernetes.client.Config.KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH;
import static io.fabric8.kubernetes.client.Config.KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH;
import static io.fabric8.kubernetes.client.utils.Utils.isNotNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static okhttp3.ConnectionSpec.CLEARTEXT;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.k8s.apiclient.ApiClientFactoryImpl;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesConfig.KubernetesConfigBuilder;
import io.harness.k8s.oidc.OidcTokenRetriever;
import io.harness.logging.LogCallback;
import io.harness.network.Http;
import io.harness.network.NoopHostnameVerifier;
import io.harness.yaml.YamlRepresenter;
import io.harness.yaml.YamlUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.constructor.SafeConstructor;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.DoneableHorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscalerList;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.internal.HorizontalPodAutoscalerOperationsImpl;
import io.fabric8.kubernetes.client.internal.SSLUtils;
import io.fabric8.kubernetes.client.utils.Utils;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.kubernetes.client.openapi.ApiClient;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationWeight;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualServiceSpec;
import me.snowdrop.istio.client.DefaultIstioClient;
import me.snowdrop.istio.client.IstioClient;
import okhttp3.Authenticator;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class KubernetesHelperService {
  @Inject private OidcTokenRetriever oidcTokenRetriever;

  public static void validateCluster(String cluster) {
    if (isBlank(cluster)) {
      throw new InvalidArgumentsException(Pair.of("Cluster", "Cluster cannot be empty"));
    }
  }

  public static void validateNamespace(String namespace) {
    if (isBlank(namespace)) {
      throw new InvalidArgumentsException(Pair.of("Namespace", "Namespace cannot be empty"));
    }

    if (namespace.length() != namespace.trim().length()) {
      throw new InvalidArgumentsException(
          Pair.of("Namespace", "[" + namespace + "] contains leading or trailing whitespaces"));
    }

    if (!ExpressionEvaluator.containsVariablePattern(namespace)) {
      try {
        new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build();
      } catch (Exception e) {
        throw new InvalidArgumentsException(
            Pair.of("Namespace",
                "\"" + namespace
                    + "\" is an invalid name. Namespaces may only contain lowercase letters, numbers, and '-'."),
            e, USER);
      }
    }
  }

  public ApiClient getApiClient(KubernetesConfig kubernetesConfig) {
    return ApiClientFactoryImpl.fromKubernetesConfig(kubernetesConfig, oidcTokenRetriever);
  }

  public KubernetesClient getKubernetesClient(KubernetesConfig kubernetesConfig) {
    return getKubernetesClient(kubernetesConfig, StringUtils.EMPTY);
  }

  /**
   * Gets a Kubernetes client.
   */
  public KubernetesClient getKubernetesClient(KubernetesConfig kubernetesConfig, String apiVersion) {
    Config config = getConfig(kubernetesConfig, apiVersion);

    String namespace = "default";
    if (isNotBlank(config.getNamespace())) {
      namespace = config.getNamespace();
    }

    OkHttpClient okHttpClient = createHttpClientWithProxySetting(config);
    try (DefaultKubernetesClient client = new DefaultKubernetesClient(okHttpClient, config)) {
      return client.inNamespace(namespace);
    }
  }

  public OpenShiftClient getOpenShiftClient(KubernetesConfig kubernetesConfig) {
    Config config = getConfig(kubernetesConfig, StringUtils.EMPTY);

    String namespace = "default";
    if (isNotBlank(config.getNamespace())) {
      namespace = config.getNamespace();
    }

    OkHttpClient okHttpClient = createHttpClientWithProxySetting(config);
    try (DefaultOpenShiftClient client = new DefaultOpenShiftClient(okHttpClient, new OpenShiftConfig(config))) {
      return client.inNamespace(namespace);
    }
  }

  public Config getConfig(KubernetesConfig kubernetesConfig, String apiVersion) {
    // Disable SSL validation (trust certs) if CA Certificate is missing in k8s configuration
    ConfigBuilder configBuilder = new ConfigBuilder().withTrustCerts(isEmpty(kubernetesConfig.getCaCert()));
    if (isNotBlank(kubernetesConfig.getNamespace())) {
      configBuilder.withNamespace(kubernetesConfig.getNamespace().trim());
    }
    if (isNotBlank(kubernetesConfig.getMasterUrl())) {
      configBuilder.withMasterUrl(kubernetesConfig.getMasterUrl().trim());
    }
    if (kubernetesConfig.getUsername() != null) {
      configBuilder.withUsername(new String(kubernetesConfig.getUsername()).trim());
    }
    if (kubernetesConfig.getPassword() != null) {
      configBuilder.withPassword(new String(kubernetesConfig.getPassword()).trim());
    }
    if (kubernetesConfig.getCaCert() != null) {
      configBuilder.withCaCertData(encode(kubernetesConfig.getCaCert()));
    }
    if (kubernetesConfig.getClientCert() != null) {
      configBuilder.withClientCertData(encode(kubernetesConfig.getClientCert()));
    }
    if (kubernetesConfig.getClientKey() != null) {
      configBuilder.withClientKeyData(encode(kubernetesConfig.getClientKey()));
    }
    if (kubernetesConfig.getClientKeyPassphrase() != null) {
      configBuilder.withClientKeyPassphrase(new String(kubernetesConfig.getClientKeyPassphrase()).trim());
    }
    if (kubernetesConfig.getServiceAccountTokenSupplier() != null) {
      configBuilder.withOauthToken(kubernetesConfig.getServiceAccountTokenSupplier().get().trim());
    }
    if (kubernetesConfig.getClientKeyAlgo() != null) {
      configBuilder.withClientKeyAlgo(kubernetesConfig.getClientKeyAlgo().trim());
    }
    configBuilder.withConnectionTimeout(30000);

    if (isNotBlank(apiVersion)) {
      configBuilder.withApiVersion(apiVersion);
    }

    Config config = configBuilder.build();
    if (KubernetesClusterAuthType.OIDC == kubernetesConfig.getAuthType()) {
      config.setOauthToken(oidcTokenRetriever.getOidcIdToken(kubernetesConfig));
    }

    return config;
  }

  public IstioClient getIstioClient(KubernetesConfig kubernetesConfig) {
    Config config = getConfig(kubernetesConfig, StringUtils.EMPTY);

    String namespace = "default";
    if (isNotBlank(config.getNamespace())) {
      namespace = config.getNamespace();
    }

    OkHttpClient okHttpClient = createHttpClientWithProxySetting(config);
    try (DefaultIstioClient client = new DefaultIstioClient(okHttpClient, config)) {
      return client.inNamespace(namespace);
    }
  }

  public static void printVirtualServiceRouteWeights(
      IstioResource virtualService, String controllerPrefix, LogCallback logCallback) {
    VirtualServiceSpec virtualServiceSpec = ((VirtualService) virtualService).getSpec();
    if (isNotEmpty(virtualServiceSpec.getHttp().get(0).getRoute())) {
      List<DestinationWeight> sorted = virtualServiceSpec.getHttp().get(0).getRoute();
      sorted.sort(Comparator.comparing(a -> Integer.valueOf(a.getDestination().getSubset())));
      for (DestinationWeight destinationWeight : sorted) {
        int weight = destinationWeight.getWeight();
        String rev = destinationWeight.getDestination().getSubset();
        logCallback.saveExecutionLog(format("   %s%s%s: %d%%", controllerPrefix, DASH, rev, weight));
      }
    } else {
      logCallback.saveExecutionLog("   None specified");
    }
  }

  /**
   * This is copied version of io.fabric8.kubernetes.client.utils.HttpClientUtils.createHttpClient()
   * with 1 addition, setting NO_PROXY flag on OkHttpClient if applicable.
   *
   * Once kubernetes library is updated to provide this support, we should get rid of this and
   * use DefaultKubernetesClient(config) constructor version as it internally call
   * super(createHttpClient(config), config)
   */
  @VisibleForTesting
  public OkHttpClient createHttpClientWithProxySetting(final Config config) {
    try {
      OkHttpClient.Builder httpClientBuilder = getOkHttpClientBuilder();
      httpClientBuilder.proxy(Http.checkAndGetNonProxyIfApplicable(config.getMasterUrl()));

      // Follow any redirects
      httpClientBuilder.followRedirects(true);
      httpClientBuilder.followSslRedirects(true);

      if (config.isTrustCerts()) {
        httpClientBuilder.hostnameVerifier(new NoopHostnameVerifier());
      }

      TrustManager[] trustManagers = SSLUtils.trustManagers(config);
      KeyManager[] keyManagers = SSLUtils.keyManagers(config);

      if (keyManagers != null || trustManagers != null || config.isTrustCerts()) {
        X509TrustManager trustManager = null;
        if (trustManagers != null && trustManagers.length == 1) {
          trustManager = (X509TrustManager) trustManagers[0];
        }

        try {
          SSLContext sslContext = SSLUtils.sslContext(keyManagers, trustManagers, config.isTrustCerts());
          httpClientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
        } catch (GeneralSecurityException e) {
          throw new AssertionError(); // The system has no TLS. Just give up.
        }
      } else {
        SSLContext context = SSLContext.getInstance("TLSv1.2");
        context.init(keyManagers, trustManagers, null);
        httpClientBuilder.sslSocketFactory(context.getSocketFactory(), (X509TrustManager) trustManagers[0]);
      }

      httpClientBuilder.addInterceptor(new Interceptor() {
        @Override
        public Response intercept(Chain chain) throws IOException {
          Request request = chain.request();
          if (isNotNullOrEmpty(config.getUsername()) && isNotNullOrEmpty(config.getPassword())) {
            Request authReq =
                chain.request()
                    .newBuilder()
                    .addHeader("Authorization", Credentials.basic(config.getUsername(), config.getPassword()))
                    .build();
            return chain.proceed(authReq);
          } else if (isNotNullOrEmpty(config.getOauthToken())) {
            Request authReq =
                chain.request().newBuilder().addHeader("Authorization", "Bearer " + config.getOauthToken()).build();
            return chain.proceed(authReq);
          }
          return chain.proceed(request);
        }
      });

      Logger reqLogger = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
      if (reqLogger.isTraceEnabled()) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        httpClientBuilder.addNetworkInterceptor(loggingInterceptor);
      }

      if (config.getConnectionTimeout() > 0) {
        httpClientBuilder.connectTimeout(config.getConnectionTimeout(), TimeUnit.MILLISECONDS);
      }

      if (config.getRequestTimeout() > 0) {
        httpClientBuilder.readTimeout(config.getRequestTimeout(), TimeUnit.MILLISECONDS);
      }

      if (config.getWebsocketPingInterval() > 0) {
        httpClientBuilder.pingInterval(config.getWebsocketPingInterval(), TimeUnit.MILLISECONDS);
      }

      if (config.getMaxConcurrentRequestsPerHost() > 0) {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(config.getMaxConcurrentRequestsPerHost());
        httpClientBuilder.dispatcher(dispatcher);
      }

      // Only check proxy if it's a full URL with protocol
      if (config.getMasterUrl().toLowerCase().startsWith(Config.HTTP_PROTOCOL_PREFIX)
          || config.getMasterUrl().startsWith(Config.HTTPS_PROTOCOL_PREFIX)) {
        try {
          URL proxyUrl = getProxyUrl(config);
          if (proxyUrl != null) {
            httpClientBuilder.proxy(
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyUrl.getHost(), proxyUrl.getPort())));

            if (config.getProxyUsername() != null) {
              httpClientBuilder.proxyAuthenticator(new Authenticator() {
                @Override
                public Request authenticate(Route route, Response response) throws IOException {
                  String credential = Credentials.basic(config.getProxyUsername(), config.getProxyPassword());
                  return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                }
              });
            }
          }

        } catch (MalformedURLException e) {
          throw new KubernetesClientException("Invalid proxy server configuration", e);
        }
      }

      if (isNotEmpty(config.getUserAgent())) {
        httpClientBuilder.addNetworkInterceptor(new Interceptor() {
          @Override
          public Response intercept(Chain chain) throws IOException {
            Request agent = chain.request().newBuilder().header("User-Agent", config.getUserAgent()).build();
            return chain.proceed(agent);
          }
        });
      }

      if (config.getTlsVersions() != null && config.getTlsVersions().length > 0) {
        ConnectionSpec spec =
            new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(config.getTlsVersions()).build();
        httpClientBuilder.connectionSpecs(asList(spec, CLEARTEXT));
      }

      return httpClientBuilder.build();
    } catch (RuntimeException | CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException
        | UnrecoverableKeyException | InvalidKeySpecException | KeyManagementException e) {
      throw KubernetesClientException.launderThrowable(e);
    }
  }

  private URL getProxyUrl(Config config) throws MalformedURLException {
    URL master = new URL(config.getMasterUrl());
    String host = master.getHost();
    if (config.getNoProxy() != null) {
      for (String noProxy : config.getNoProxy()) {
        if (host.endsWith(noProxy)) {
          return null;
        }
      }
    }
    String proxy = config.getHttpsProxy();
    if (master.getProtocol().equals("http")) {
      proxy = config.getHttpProxy();
    }
    if (proxy != null) {
      return new URL(proxy);
    }
    return null;
  }

  public static String encode(char[] value) {
    String encodedValue = new String(value).trim();
    if (isNotBlank(encodedValue) && encodedValue.startsWith("-----BEGIN ")) {
      encodedValue = encodeBase64(encodedValue);
    }
    return encodedValue;
  }

  public static String toYaml(Object entity) throws JsonProcessingException {
    return new ObjectMapper(new YAMLFactory().configure(WRITE_DOC_START_MARKER, false))
        .setSerializationInclusion(NON_EMPTY)
        .writeValueAsString(entity);
  }

  public static String toDisplayYaml(Object entity) {
    Yaml yaml = new Yaml(new SafeConstructor(), new YamlRepresenter(true), YamlUtils.getDumperOptions());
    return YamlUtils.cleanupYaml(yaml.dump(entity));
  }

  public NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>>
  hpaOperationsForCustomMetricHPA(KubernetesConfig kubernetesConfig, String apiName) {
    DefaultKubernetesClient kubernetesClient = (DefaultKubernetesClient) getKubernetesClient(kubernetesConfig, apiName);

    /*
     * Following constructor invocation content is copied from HorizontalPodAutoscalerOperationsImpl(OkHttpClient
     * client, Config config, String namespace){...}, except we are passing apiName, where as in above mentioned one its
     * hardcoded as "v1".
     *
     * Following call does exactly what
     * getKubernetesClient(kubernetesConfig,encryptedDataDetails).autoscaling().horizontalPodAutoscalers()) does, Only
     * diff is, here its based on apiVersion we passed. So for "v2beta1" version, we needed to take this approach, as
     * there was this issue with fabric8 library, that
     * getKubernetesClient(kubernetesConfig,encryptedDataDetails).autoscaling().horizontalPodAutoscalers()) always
     * returns client with "v1" apiVersion.
     * */
    MixedOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
        Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>> mixedOperation =
        new HorizontalPodAutoscalerOperationsImpl(kubernetesClient.getHttpClient(), kubernetesClient.getConfiguration(),
            apiName, kubernetesClient.getNamespace(), null, true, null, null, false, -1, new TreeMap<>(),
            new TreeMap<>(), new TreeMap<>(), new TreeMap<>(), new TreeMap<>());

    return mixedOperation.inNamespace(kubernetesConfig.getNamespace());
  }

  public NonNamespaceOperation<HorizontalPodAutoscaler, HorizontalPodAutoscalerList, DoneableHorizontalPodAutoscaler,
      Resource<HorizontalPodAutoscaler, DoneableHorizontalPodAutoscaler>>
  hpaOperations(KubernetesConfig kubernetesConfig) {
    return getKubernetesClient(kubernetesConfig)
        .autoscaling()
        .horizontalPodAutoscalers()
        .inNamespace(kubernetesConfig.getNamespace());
  }

  /**
   * Separates apiVersion for apiGroup/apiVersion combination.
   * @param apiVersion  The apiVersion or apiGroup/apiVersion combo.
   * @return            Just the apiVersion part without the apiGroup.
   */
  public String trimVersion(String apiVersion) {
    if (apiVersion == null) {
      return null;
    } else {
      String[] versionParts = apiVersion.split("/");
      return versionParts[versionParts.length - 1];
    }
  }

  public static KubernetesConfig getKubernetesConfigFromServiceAccount(String namespace) {
    KubernetesConfigBuilder kubernetesConfigBuilder = KubernetesConfig.builder().namespace(namespace);

    String masterHost = Utils.getSystemPropertyOrEnvVar("KUBERNETES_SERVICE_HOST", (String) null);
    String masterPort = Utils.getSystemPropertyOrEnvVar("KUBERNETES_SERVICE_PORT", (String) null);
    if (masterHost != null && masterPort != null) {
      String hostPort = joinHostPort(masterHost, masterPort);
      kubernetesConfigBuilder.masterUrl("https://" + hostPort);
    }

    String caCert = getFileContent(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH);
    if (isNotBlank(caCert)) {
      kubernetesConfigBuilder.caCert(caCert.toCharArray());
    }

    String serviceAccountToken = getFileContent(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH);
    if (isNotBlank(serviceAccountToken)) {
      kubernetesConfigBuilder.serviceAccountTokenSupplier(() -> serviceAccountToken);
    }

    return kubernetesConfigBuilder.build();
  }

  public static KubernetesConfig getKubernetesConfigFromDefaultKubeConfigFile(String namespace) {
    KubernetesConfigBuilder kubernetesConfigBuilder = KubernetesConfig.builder().namespace(namespace);

    File kubeConfigFile = new File(Utils.getSystemPropertyOrEnvVar(
        KUBERNETES_KUBECONFIG_FILE, new File(getHomeDir(), ".kube" + File.separator + "config").toString()));
    boolean kubeConfigFileExists = Files.isRegularFile(kubeConfigFile.toPath());

    if (kubeConfigFileExists) {
      String kubeconfigContents;
      try {
        Path kubeConfigPath = kubeConfigFile.toPath();
        kubeconfigContents = new String(Files.readAllBytes(kubeConfigPath), StandardCharsets.UTF_8);
        Config config = Config.fromKubeconfig(null, kubeconfigContents, kubeConfigFile.getPath());

        return kubernetesConfigBuilder.masterUrl(config.getMasterUrl())
            .username(getCharArray(config.getUsername()))
            .password(getCharArray(config.getPassword()))
            .caCert(getCharArray(config.getCaCertData()))
            .clientCert(getCharArray(config.getClientCertData()))
            .clientKey(getCharArray(config.getClientKeyData()))
            .clientKeyPassphrase(getCharArray(config.getClientKeyPassphrase()))
            .serviceAccountTokenSupplier(config.getOauthToken() != null ? config::getOauthToken : null)
            .clientKeyAlgo(config.getClientKeyAlgo())
            .build();
      } catch (IOException e) {
        log.error("Could not load Kubernetes config file from {}", kubeConfigFile.getPath(), e);
      }
    }
    return kubernetesConfigBuilder.build();
  }

  private static char[] getCharArray(String input) {
    if (input == null) {
      return null;
    }
    return input.toCharArray();
  }

  public static boolean isRunningInCluster() {
    return Utils.getSystemPropertyOrEnvVar("KUBERNETES_SERVICE_HOST", (String) null) != null;
  }

  private static String getFileContent(String filename) {
    try {
      if (Files.isRegularFile(new File(filename).toPath())) {
        return new String(Files.readAllBytes(new File(filename).toPath()), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      noop(); // Ignore
    }
    return "";
  }
}
