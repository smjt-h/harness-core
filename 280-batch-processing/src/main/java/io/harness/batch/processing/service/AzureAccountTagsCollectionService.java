/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.service;

import static io.harness.batch.processing.pricing.gcp.bigquery.BQConst.CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.DATA_SET_NAME_TEMPLATE;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.getDataSetDescription;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.batch.processing.config.AzureStorageSyncConfig;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.pricing.gcp.bigquery.BQConst;
import io.harness.batch.processing.service.CloudProviderEntityTagsCollectionUtil;
import io.harness.batch.processing.shard.AccountShardService;
import io.harness.batch.processing.tasklet.dto.CloudProviderEntityTags;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.CcmConnectorFilter;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ceazure.CEAzureConnectorDTO;
import io.harness.filter.FilterType;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.RestCallToNGManagerClientUtils;

import software.wings.beans.Account;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.fluent.models.TagsResourceInner;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TimePartitioning;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Singleton
@Slf4j
public class AzureAccountTagsCollectionService {
  @Autowired private BatchMainConfig mainConfig;
  @Autowired private AccountShardService accountShardService;
  @Autowired BigQueryService bigQueryService;
  @Autowired CloudProviderEntityTagsCollectionUtil cloudProviderEntityTagsCollectionUtil;
  @Autowired private ConnectorResourceClient connectorResourceClient;

  public void update() {
    List<Account> accounts = accountShardService.getCeEnabledAccounts();
    log.info("accounts {} size: {}", accounts, accounts.size());
    for (Account account : accounts) {
      log.info("Fetching connectors for accountName {}, accountId {}", account.getAccountName(), account.getUuid());
      List<ConnectorResponseDTO> nextGenConnectorResponses =
          getNextGenConnectorResponses(account.getUuid(), ConnectorType.CE_AZURE);
      for (ConnectorResponseDTO connector : nextGenConnectorResponses) {
        ConnectorInfoDTO connectorInfo = connector.getConnector();
        CEAzureConnectorDTO ceAzureConnectorDTO = (CEAzureConnectorDTO) connectorInfo.getConnectorConfig();
        try {
          processAndInsertTags(ceAzureConnectorDTO, account);
        } catch (Exception e) {
          log.warn("Exception processing azure tags for connectorId: {} for CCM accountId: {}",
              connectorInfo.getIdentifier(), account.getUuid(), e);
        }
      }
    }
  }

  public void processAndInsertTags(CEAzureConnectorDTO ceAzureConnectorDTO, Account account) {
    String tableName = createBQTable(account); // This can be moved to connector creation part
    AzureStorageSyncConfig azureStorageSyncConfig = mainConfig.getAzureStorageSyncConfig();
    log.info("Processing tags for azureTenantID: {} azureSubscriptionId: {}, storage: {}",
        ceAzureConnectorDTO.getTenantId(), ceAzureConnectorDTO.getSubscriptionId(),
        ceAzureConnectorDTO.getBillingExportSpec().getStorageAccountName());
    Map<String, String> azureTags = null;
    System.out.println(azureTags);
    List<CloudProviderEntityTags> cloudProviderEntityTags = new ArrayList<>();
    try {
      ClientSecretCredential clientSecretCredential =
          new ClientSecretCredentialBuilder()
              .clientId(azureStorageSyncConfig.getAzureAppClientId())
              .clientSecret(azureStorageSyncConfig.getAzureAppClientSecret())
              .tenantId(ceAzureConnectorDTO.getTenantId())
              .build();

      AzureProfile profile = new AzureProfile(AzureEnvironment.AZURE);

      AzureResourceManager azure = AzureResourceManager.authenticate(clientSecretCredential, profile)
                                       .withSubscription(ceAzureConnectorDTO.getSubscriptionId());
      TagsResourceInner tagsResourceInner =
          azure.genericResources().manager().serviceClient().getTagOperations().getAtScope(
              format("/subscriptions/%s/", ceAzureConnectorDTO.getSubscriptionId()));
      azureTags = tagsResourceInner.properties().tags();
    } catch (Exception ex) {
      log.error("Error", ex);
    }
    System.out.println(azureTags);
    if (isNotEmpty(azureTags)) {
      for (Map.Entry<String, String> entry : azureTags.entrySet()) {
        cloudProviderEntityTags.add(
            CloudProviderEntityTags.builder().key(entry.getKey()).value(entry.getValue()).build());
      }
    }
    insertInBQ(
        tableName, "AZURE", ceAzureConnectorDTO.getSubscriptionId(), "subscription", "", cloudProviderEntityTags);
  }

  PageResponse getConnectors(
      String accountId, int page, int size, ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO) {
    return RestCallToNGManagerClientUtils.execute(
        connectorResourceClient.listConnectors(accountId, null, null, page, size, connectorFilterPropertiesDTO, false));
  }

  public String createBQTable(Account account) {
    String accountId = account.getUuid();
    String accountName = account.getAccountName();
    String dataSetName =
        String.format(DATA_SET_NAME_TEMPLATE, BillingDataPipelineUtils.modifyStringToComplyRegex(accountId));
    String description = getDataSetDescription(accountId, accountName);
    BigQuery bigquery = bigQueryService.get();
    String tableName = format("%s.%s.%s", mainConfig.getBillingDataPipelineConfig().getGcpProjectId(), dataSetName,
        CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    log.info("DatasetName: {} , TableName {}", dataSetName, tableName);
    try {
      DatasetInfo datasetInfo = DatasetInfo.newBuilder(dataSetName).setDescription(description).build();
      Dataset createdDataSet = bigquery.create(datasetInfo);
      log.info("Dataset created {}", createdDataSet);
      bigquery.create(getCloudProviderEntityTagsTableInfo(dataSetName));
      log.info("Table created {}", CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    } catch (BigQueryException bigQueryEx) {
      // dataset/table already exists.
      if (bigquery.getTable(TableId.of(dataSetName, CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME)) == null) {
        bigquery.create(getCloudProviderEntityTagsTableInfo(dataSetName));
        log.info("Table created {}", CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
        return tableName;
      }
      log.warn("Error code {}:", bigQueryEx.getCode(), bigQueryEx);
    }
    return tableName;
  }

  protected static TableInfo getCloudProviderEntityTagsTableInfo(String dataSetName) {
    TableId tableId = TableId.of(dataSetName, CLOUD_PROVIDER_ENTITY_TAGS_TABLE_NAME);
    TimePartitioning partitioning =
        TimePartitioning.newBuilder(TimePartitioning.Type.DAY).setField("updatedAt").build();
    Schema schema = Schema.of(Field.of("cloudProviderId", StandardSQLTypeName.STRING),
        Field.of("updatedAt", StandardSQLTypeName.TIMESTAMP), Field.of("entityType", StandardSQLTypeName.STRING),
        Field.of("entityId", StandardSQLTypeName.STRING), Field.of("entityName", StandardSQLTypeName.STRING),
        Field
            .newBuilder("labels", StandardSQLTypeName.STRUCT, Field.of("key", StandardSQLTypeName.STRING),
                Field.of("value", StandardSQLTypeName.STRING))
            .setMode(Field.Mode.REPEATED)
            .build());
    StandardTableDefinition tableDefinition =
        StandardTableDefinition.newBuilder().setSchema(schema).setTimePartitioning(partitioning).build();
    return TableInfo.newBuilder(tableId, tableDefinition).build();
  }

  public List<ConnectorResponseDTO> getNextGenConnectorResponses(String accountId, ConnectorType connectorType) {
    List<ConnectorResponseDTO> nextGenConnectorResponses = new ArrayList<>();
    PageResponse<ConnectorResponseDTO> response = null;
    ConnectorFilterPropertiesDTO connectorFilterPropertiesDTO =
        ConnectorFilterPropertiesDTO.builder()
            .types(Arrays.asList(connectorType))
            .ccmConnectorFilter(CcmConnectorFilter.builder().featuresEnabled(Arrays.asList(CEFeatures.BILLING)).build())
            .build();
    connectorFilterPropertiesDTO.setFilterType(FilterType.CONNECTOR);
    int page = 0;
    int size = 100;
    try {
      do {
        response = getConnectors(accountId, page, size, connectorFilterPropertiesDTO);
        log.info("getConnectors response ", response);
        if (response != null && isNotEmpty(response.getContent())) {
          nextGenConnectorResponses.addAll(response.getContent());
        }
        page++;
      } while (response != null && isNotEmpty(response.getContent()));
      log.info("Processing batch size of {} for account {} and connectorType {}", nextGenConnectorResponses.size(),
          accountId, connectorType);
      return nextGenConnectorResponses;
    } catch (Exception ex) {
      log.warn("Error", ex);
    }
    return nextGenConnectorResponses;
  }

  public String getTagsBQFormat(List<CloudProviderEntityTags> cloudProviderEntityTagsList) {
    StringBuilder tagsBQFormat = new StringBuilder();
    if (isNotEmpty(cloudProviderEntityTagsList)) {
      tagsBQFormat.append("ARRAY[");
      String prefix = "";
      for (CloudProviderEntityTags cloudProviderEntityTags : cloudProviderEntityTagsList) {
        tagsBQFormat.append(prefix);
        prefix = ",";
        tagsBQFormat.append(
            format(" STRUCT('%s', '%s')", cloudProviderEntityTags.getKey(), cloudProviderEntityTags.getValue()));
      }
      tagsBQFormat.append(']');
      return tagsBQFormat.toString();
    } else {
      return null;
    }
  }

  public void insertInBQ(String tableName, String cloudProviderId, String entityId, String entityType,
      String entityName, List<CloudProviderEntityTags> cloudProviderEntityTagsList) {
    String tagsBQFormat = getTagsBQFormat(cloudProviderEntityTagsList);
    String formattedQuery =
        format(BQConst.CLOUD_PROVIDER_ENTITY_TAGS_INSERT, tableName, cloudProviderId, entityId, entityType, tableName,
            cloudProviderId, entityId, entityType, entityName, tagsBQFormat, Instant.now().toString());
    log.info("Inserting tags in BQ for entityId: {} query: '{}'", entityId, formattedQuery);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(formattedQuery).build();
    try {
      bigQueryService.get().query(queryConfig);
      log.info("Inserted tags in BQ for entityId: {}", entityId);
    } catch (BigQueryException | InterruptedException bigQueryException) {
      log.warn("Error: ", bigQueryException);
    }
  }
}
