/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.helpers.ext.azure;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.AZURE_SERVICE_EXCEPTION;
import static io.harness.eraro.ErrorCode.CLUSTER_NOT_FOUND;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;
import static io.harness.validation.Validator.notNullCheck;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.exception.AzureServiceException;
import io.harness.exception.ClusterNotFoundException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubeConfigHelper;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.network.Http;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;
import software.wings.beans.AzureContainerRegistry;
import software.wings.beans.AzureImageDefinition;
import software.wings.beans.AzureImageGallery;
import software.wings.beans.AzureImageVersion;
import software.wings.beans.AzureKubernetesCluster;
import software.wings.beans.AzureTagDetails;
import software.wings.service.intfc.security.EncryptionService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.containerservice.KubernetesCluster;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import com.microsoft.rest.LogLevel;
import io.kubernetes.client.util.KubeConfig;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.http.HttpStatus;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class AzureDelegateHelperService {
  private static final int CONNECT_TIMEOUT = 5; // TODO:: read from config
  private static final int READ_TIMEOUT = 10;

  @Inject private EncryptionService encryptionService;

  public boolean isValidKubernetesCluster(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String resourceGroup, String clusterName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    KubernetesCluster cluster =
        getAzureClient(azureConfig, subscriptionId).kubernetesClusters().getByResourceGroup(resourceGroup, clusterName);
    return cluster != null;
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, AzureKubernetesCluster azureKubernetesCluster, String namespace,
      boolean isInstanceSync) {
    return getKubernetesClusterConfig(azureConfig, encryptionDetails, azureKubernetesCluster.getSubscriptionId(),
        azureKubernetesCluster.getResourceGroup(), azureKubernetesCluster.getName(), namespace, isInstanceSync);
  }

  public KubernetesConfig getKubernetesClusterConfig(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroup, String clusterName,
      String namespace, boolean isInstanceSync) {
    encryptionService.decrypt(azureConfig, encryptionDetails, isInstanceSync);
    try {
      Response<AksGetCredentialsResponse> response =
          getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
              .getAdminCredentials(getAzureBearerAuthToken(azureConfig), subscriptionId, resourceGroup, clusterName)
              .execute();

      if (response.isSuccessful()) {
        return parseConfig(
            response.body().getProperties().getKubeConfig(), isNotBlank(namespace) ? namespace : "default");
      } else {
        String errorMessage =
            "Error occurred while getting KubernetesClusterConfig from subscriptionId/resourceGroup/clusterName :"
            + subscriptionId + "/" + resourceGroup + "/" + clusterName + response.raw();
        log.error(errorMessage);
        int statusCode = response.code();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
          throw new ClusterNotFoundException(errorMessage, CLUSTER_NOT_FOUND, USER);
        } else {
          throw new AzureServiceException(response.message(), AZURE_SERVICE_EXCEPTION, USER);
        }
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  public String getUrl(String acrHostName) {
    return "https://" + acrHostName + (acrHostName.endsWith("/") ? "" : "/");
  }

  public boolean isValidContainerRegistry(AzureConfig azureConfig, String subscriptionId, String registryName) {
    return getAzureClient(azureConfig, subscriptionId)
               .containerRegistries()
               .list()
               .stream()
               .filter(registry -> registry.name().equalsIgnoreCase(registryName))
               .count()
        != 0;
  }

  public List<AzureContainerRegistry> listContainerRegistries(AzureConfig azureConfig, String subscriptionId) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<AzureContainerRegistry> registries = new ArrayList<>();
    azure.containerRegistries().list().forEach(registry
        -> registries.add(AzureContainerRegistry.builder()
                              .id(registry.id())
                              .name(registry.name())
                              .resourceGroup(registry.resourceGroupName())
                              .subscriptionId(subscriptionId)
                              .loginServer(registry.loginServerUrl())
                              .build()));
    return registries;
  }

  public boolean isValidSubscription(AzureConfig azureConfig, String subscriptionId) {
    return getAzureClient(azureConfig)
               .subscriptions()
               .list()
               .stream()
               .filter(subscription -> subscription.subscriptionId().equalsIgnoreCase(subscriptionId))
               .count()
        != 0;
  }

  public Map<String, String> listSubscriptions(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    Azure azure = getAzureClient(azureConfig);
    return azure.subscriptions().list().stream().collect(
        Collectors.toMap(Subscription::subscriptionId, Subscription::displayName));
  }

  public Set<String> listResourceGroups(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);

    try {
      Azure azure = getAzureClient(azureConfig, subscriptionId);
      notNullCheck("Azure Client", azure);
      List<ResourceGroup> resourceGroupList = azure.resourceGroups().list();
      return resourceGroupList.stream().map(HasName::name).collect(Collectors.toSet());
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return Collections.EMPTY_SET;
  }

  public List<AzureTagDetails> listTags(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails, String subscriptionId) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    try {
      Response<AzureListTagsResponse> response = getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
                                                     .listTags(getAzureBearerAuthToken(azureConfig), subscriptionId)
                                                     .execute();

      if (response.isSuccessful()) {
        return response.body()
            .getValue()
            .stream()
            .map(tagDetails
                -> AzureTagDetails.builder()
                       .tagName(tagDetails.getTagName())
                       .values(tagDetails.getValues().stream().map(TagValue::getTagValue).collect(toList()))
                       .build())
            .collect(toList());
      } else {
        log.error("Error occurred while getting Tags from subscriptionId : " + subscriptionId
            + " Response: " + response.raw());
        throw new AzureServiceException(response.message(), AZURE_SERVICE_EXCEPTION, USER);
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
      return null;
    }
  }

  public Set<String> listTagsBySubscription(
      String subscriptionId, AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    try {
      Response<AzureListTagsResponse> response = getAzureManagementRestClient(azureConfig.getAzureEnvironmentType())
                                                     .listTags(getAzureBearerAuthToken(azureConfig), subscriptionId)
                                                     .execute();

      if (response.isSuccessful()) {
        return response.body().getValue().stream().map(TagDetails::getTagName).collect(toSet());
      } else {
        log.error("Error occurred while getting Tags from subscriptionId : " + subscriptionId
            + " Response: " + response.raw());
        throw new AzureServiceException(response.message(), AZURE_SERVICE_EXCEPTION, USER);
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
      return null;
    }
  }

  public List<String> listRepositories(AzureConfig azureConfig, String subscriptionId, String registryName) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    try {
      Registry registry = azure.containerRegistries()
                              .list()
                              .stream()
                              .filter(item -> item.name().equals(registryName))
                              .findFirst()
                              .get();
      AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
      List<String> allRepositories = new ArrayList<>();
      String last = null;
      List<String> repositories;
      do {
        repositories =
            acrRestClient
                .listRepositories(getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), last)
                .execute()
                .body()
                .getRepositories();
        if (isNotEmpty(repositories)) {
          allRepositories.addAll(repositories);
          last = repositories.get(repositories.size() - 1);
        }
      } while (isNotEmpty(repositories));
      return allRepositories.stream().distinct().collect(toList());
    } catch (Exception e) {
      log.error("Error occurred while getting repositories from subscriptionId/registryName :" + subscriptionId + "/"
              + registryName,
          e);
      throw new AzureServiceException(
          "Failed to list repositories " + ExceptionUtils.getMessage(e), AZURE_SERVICE_EXCEPTION, USER);
    }
  }

  public List<AzureImageGallery> listImageGalleries(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroupName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    return azure.galleries()
        .listByResourceGroup(resourceGroupName)
        .stream()
        .map(ig
            -> AzureImageGallery.builder()
                   .name(ig.name())
                   .subscriptionId(subscriptionId)
                   .resourceGroupName(resourceGroupName)
                   .regionName(ig.regionName())
                   .build())
        .collect(Collectors.toList());
  }

  public List<String> listRepositoryTags(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName, String repositoryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    try {
      Registry registry = azure.containerRegistries()
                              .list()
                              .stream()
                              .filter(item -> item.name().equals(registryName))
                              .findFirst()
                              .get();
      AcrRestClient acrRestClient = getAcrRestClient(registry.loginServerUrl());
      return acrRestClient
          .listRepositoryTags(
              getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), repositoryName)
          .execute()
          .body()
          .getTags();
    } catch (Exception e) {
      log.error("Error occurred while getting repositories from subscriptionId/registryName/repositoryName :"
              + subscriptionId + "/" + registryName + "/" + repositoryName,
          e);
      throw new AzureServiceException(
          "Failed to retrieve repository tags " + ExceptionUtils.getMessage(e), AZURE_SERVICE_EXCEPTION, USER);
    }
  }

  public List<String> listRepositoryTags(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String registryHostName, String repositoryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    try {
      AcrRestClient acrRestClient = getAcrRestClient(registryHostName);
      return acrRestClient
          .listRepositoryTags(
              getAuthHeader(azureConfig.getClientId(), new String(azureConfig.getKey())), repositoryName)
          .execute()
          .body()
          .getTags();
    } catch (Exception e) {
      log.error("Error occurred while getting Tags for Repository :" + registryHostName + "/" + repositoryName, e);
      throw new AzureServiceException(
          "Failed to retrieve repository tags " + ExceptionUtils.getMessage(e), AZURE_SERVICE_EXCEPTION, USER);
    }
  }

  public List<String> listContainerRegistryNames(AzureConfig azureConfig, String subscriptionId) {
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    List<String> registries = new ArrayList<>();
    azure.containerRegistries().list().forEach(registry -> registries.add(registry.name()));
    return registries;
  }

  public String getLoginServerForRegistry(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    return getRegistry(azureConfig, encryptionDetails, subscriptionId, registryName).loginServerUrl();
  }

  private Registry getRegistry(AzureConfig azureConfig, List<EncryptedDataDetail> encryptionDetails,
      String subscriptionId, String registryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    return getAzureClient(azureConfig, subscriptionId)
        .containerRegistries()
        .list()
        .stream()
        .filter(item -> item.name().equals(registryName))
        .findFirst()
        .get();
  }

  AzureManagementRestClient getAzureManagementRestClient(AzureEnvironmentType azureEnvironmentType) {
    String url = getAzureEnvironment(azureEnvironmentType).resourceManagerEndpoint();
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AzureManagementRestClient.class);
  }

  private AzureEnvironment getAzureEnvironment(AzureEnvironmentType azureEnvironmentType) {
    if (azureEnvironmentType == null) {
      return AzureEnvironment.AZURE;
    }

    switch (azureEnvironmentType) {
      case AZURE_US_GOVERNMENT:
        return AzureEnvironment.AZURE_US_GOVERNMENT;

      case AZURE:
      default:
        return AzureEnvironment.AZURE;
    }
  }

  @VisibleForTesting
  String getAzureBearerAuthToken(AzureConfig azureConfig) {
    try {
      AzureEnvironment azureEnvironment = getAzureEnvironment(azureConfig.getAzureEnvironmentType());
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
          azureConfig.getClientId(), azureConfig.getTenantId(), new String(azureConfig.getKey()), azureEnvironment);

      String token = credentials.getToken(azureEnvironment.managementEndpoint());
      return "Bearer " + token;
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  private AcrRestClient getAcrRestClient(String registryHostName) {
    String url = getUrl(registryHostName);
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(AcrRestClient.class);
  }

  @VisibleForTesting
  protected Azure getAzureClient(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials =
          new ApplicationTokenCredentials(azureConfig.getClientId(), azureConfig.getTenantId(),
              new String(azureConfig.getKey()), getAzureEnvironment(azureConfig.getAzureEnvironmentType()));

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  @VisibleForTesting
  protected Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    try {
      ApplicationTokenCredentials credentials =
          new ApplicationTokenCredentials(azureConfig.getClientId(), azureConfig.getTenantId(),
              new String(azureConfig.getKey()), getAzureEnvironment(azureConfig.getAzureEnvironmentType()));

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  private String getAuthHeader(String username, String password) {
    return "Basic " + encodeBase64String(format("%s:%s", username, password).getBytes(UTF_8));
  }

  private KubernetesConfig parseConfig(String configContent, String namespace) {
    try {
      KubeConfig kubeConfig = KubeConfig.loadKubeConfig(new StringReader(decodeBase64ToString(configContent)));
      String masterUrl = kubeConfig.getServer();
      String certificateAuthorityData = kubeConfig.getCertificateAuthorityData();
      String username = KubeConfigHelper.getCurrentUser(kubeConfig);
      String clientCertificateData = kubeConfig.getClientCertificateData();
      String clientKeyData = kubeConfig.getClientKeyData();

      return KubernetesConfig.builder()
          .namespace(namespace)
          .masterUrl(masterUrl)
          .caCert(certificateAuthorityData.toCharArray())
          .username(username != null ? username.toCharArray() : null)
          .clientCert(clientCertificateData.toCharArray())
          .clientKey(clientKeyData.toCharArray())
          .build();
    } catch (Exception e) {
      throw new AzureServiceException(
          "Failed to create kubernetes configuration " + ExceptionUtils.getMessage(e), AZURE_SERVICE_EXCEPTION, USER);
    }
  }

  public void handleAzureAuthenticationException(Exception e) {
    log.error("HandleAzureAuthenticationException: Exception:" + e);

    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException) {
        throw new InvalidRequestException("Invalid Azure credentials.", USER);
      }
    }

    throw new InvalidRequestException("Failed to connect to Azure cluster. " + ExceptionUtils.getMessage(e), USER);
  }

  public List<AzureImageVersion> listImageDefinitionVersions(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroupName, String galleryName,
      String imageDefinition) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    // Only fetch successful Azure Image versions
    return azure.galleryImageVersions()
        .listByGalleryImage(resourceGroupName, galleryName, imageDefinition)
        .stream()
        .filter(id -> "Succeeded".equals(id.provisioningState()))
        .map(id
            -> AzureImageVersion.builder()
                   .name(id.name())
                   .imageDefinitionName(imageDefinition)
                   .location(id.location())
                   .subscriptionId(subscriptionId)
                   .resourceGroupName(resourceGroupName)
                   .galleryName(galleryName)
                   .build())
        .collect(Collectors.toList());
  }

  public List<AzureImageDefinition> listImageDefinitions(AzureConfig azureConfig,
      List<EncryptedDataDetail> encryptionDetails, String subscriptionId, String resourceGroupName,
      String galleryName) {
    encryptionService.decrypt(azureConfig, encryptionDetails, false);
    Azure azure = getAzureClient(azureConfig, subscriptionId);
    return azure.galleryImages()
        .listByGallery(resourceGroupName, galleryName)
        .stream()
        .map(id
            -> AzureImageDefinition.builder()
                   .name(id.name())
                   .location(id.location())
                   .osType(id.osType().name())
                   .subscriptionId(subscriptionId)
                   .resourceGroupName(resourceGroupName)
                   .galleryName(galleryName)
                   .build())
        .collect(Collectors.toList());
  }
}
