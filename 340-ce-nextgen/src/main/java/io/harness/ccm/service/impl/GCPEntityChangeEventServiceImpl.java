/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.service.impl;

import static io.harness.eventsframework.EventsFrameworkMetadataConstants.CREATE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.UPDATE_ACTION;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.CENextGenConfiguration;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.beans.config.GcpConfig;
import io.harness.ccm.service.intf.GCPEntityChangeEventService;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResourceClient;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.gcpccm.GcpCloudCostConnectorDTO;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.remote.client.NGRestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CE)
public class GCPEntityChangeEventServiceImpl implements GCPEntityChangeEventService {
  public static final String ACCOUNT_ID = "accountId";
  public static final String GCP_INFRA_PROJECT_ID = "gcpInfraProjectId";
  public static final String GCP_INFRA_SERVICE_ACCOUNT_EMAIL = "gcpInfraServiceAccountEmail";
  public static final String CONNECTOR_ID = "connectorId";
  public static final String ACTION = "action";
  private static final String GOOGLE_CREDENTIALS_PATH = "CE_GCP_CREDENTIALS_PATH";
  @Inject ConnectorResourceClient connectorResourceClient;
  @Inject CENextGenConfiguration configuration;
  @Inject BigQueryService bigQueryService;

  @Override
  public boolean processGCPEntityCreateEvent(EntityChangeDTO entityChangeDTO) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    ArrayList<ImmutableMap<String, String>> entityChangeEvents = new ArrayList<>();

    GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
        (GcpCloudCostConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
    if (isVisibilityFeatureEnabled(gcpCloudCostConnectorDTO)) {
      updateEventData(CREATE_ACTION, identifier, accountIdentifier, gcpCloudCostConnectorDTO.getProjectId(),
          gcpCloudCostConnectorDTO.getServiceAccountEmail(), entityChangeEvents);
      publishMessage(entityChangeEvents);
    }
    log.info("GcpCloudCostConnectorDTO: {}", gcpCloudCostConnectorDTO);
    // todo: remove below log
    log.info("CREATE action event processed for id: {}, accountId: {}", identifier, accountIdentifier);
    return true;
  }

  @Override
  public boolean processGCPEntityUpdateEvent(EntityChangeDTO entityChangeDTO) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    ArrayList<ImmutableMap<String, String>> entityChangeEvents = new ArrayList<>();

    GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO =
        (GcpCloudCostConnectorDTO) getConnectorConfigDTO(accountIdentifier, identifier).getConnectorConfig();
    if (isVisibilityFeatureEnabled(gcpCloudCostConnectorDTO)) {
      updateEventData(UPDATE_ACTION, identifier, accountIdentifier, gcpCloudCostConnectorDTO.getProjectId(),
          gcpCloudCostConnectorDTO.getServiceAccountEmail(), entityChangeEvents);
      publishMessage(entityChangeEvents);
    }
    // todo: remove below log
    log.info("UPDATE action event processed for id: {}, accountId: {}", identifier, accountIdentifier);
    return true;
  }

  @Override
  public boolean processGCPEntityDeleteEvent(EntityChangeDTO entityChangeDTO) {
    String identifier = entityChangeDTO.getIdentifier().getValue();
    String accountIdentifier = entityChangeDTO.getAccountIdentifier().getValue();
    ArrayList<ImmutableMap<String, String>> entityChangeEvents = new ArrayList<>();
    updateEventData(DELETE_ACTION, identifier, accountIdentifier, "", "", entityChangeEvents);
    publishMessage(entityChangeEvents);
    // todo: remove below log
    log.info("DELETE action event processed for id: {}, accountId: {}", identifier, accountIdentifier);
    return true;
  }

  private boolean isVisibilityFeatureEnabled(GcpCloudCostConnectorDTO gcpCloudCostConnectorDTO) {
    List<CEFeatures> featuresEnabled = gcpCloudCostConnectorDTO.getFeaturesEnabled();
    return featuresEnabled.contains(CEFeatures.VISIBILITY);
  }

  private void updateEventData(String action, String identifier, String accountIdentifier, String gcpInfraProjectId,
      String gcpInfraServiceAccountEmail, ArrayList<ImmutableMap<String, String>> entityChangeEvents) {
    log.info("Visibility feature is enabled. Prepping event for pubsub");
    entityChangeEvents.add(ImmutableMap.<String, String>builder()
                               .put(ACTION, action)
                               .put(ACCOUNT_ID, accountIdentifier)
                               .put(GCP_INFRA_PROJECT_ID, gcpInfraProjectId)
                               .put(GCP_INFRA_SERVICE_ACCOUNT_EMAIL, gcpInfraServiceAccountEmail)
                               .put(CONNECTOR_ID, identifier)
                               .build());
  }

  public ConnectorInfoDTO getConnectorConfigDTO(String accountIdentifier, String connectorIdentifierRef) {
    try {
      Optional<ConnectorDTO> connectorDTO =
          NGRestUtils.getResponse(connectorResourceClient.get(connectorIdentifierRef, accountIdentifier, null, null));

      if (!connectorDTO.isPresent()) {
        throw new InvalidRequestException(format("Connector not found for identifier : [%s]", connectorIdentifierRef));
      }

      return connectorDTO.get().getConnectorInfo();
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Error while getting connector information : [%s]", connectorIdentifierRef));
    }
  }

  public void publishMessage(ArrayList<ImmutableMap<String, String>> entityChangeEvents) {
    if (entityChangeEvents.isEmpty()) {
      log.info("Visibility is not enabled. Not sending event");
      return;
    }
    GcpConfig gcpConfig = configuration.getGcpConfig();
    String harnessGcpProjectId = gcpConfig.getGcpProjectId();
    String inventoryPubSubTopic = gcpConfig.getGcpGcpConnectorCrudPubSubTopic();
    ServiceAccountCredentials sourceGcpCredentials = bigQueryService.getCredentials(GOOGLE_CREDENTIALS_PATH);
    TopicName topicName = TopicName.of(harnessGcpProjectId, inventoryPubSubTopic);
    Publisher publisher = null;
    log.info("Publishing event to topic: {}", topicName);
    try {
      // Create a publisher instance with default settings bound to the topic
      publisher = Publisher.newBuilder(topicName)
                      .setCredentialsProvider(FixedCredentialsProvider.create(sourceGcpCredentials))
                      .build();
      ObjectMapper objectMapper = new ObjectMapper();
      String message = objectMapper.writeValueAsString(entityChangeEvents);
      ByteString data = ByteString.copyFromUtf8(message);
      log.info("Sending event with data: {}", data);
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      // Once published, returns a server-assigned message id (unique within the topic)
      ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
      String messageId = messageIdFuture.get();
      log.info("Published event with data: {}, messageId: {}", message, messageId);
    } catch (Exception e) {
      log.error("Error occurred while sending event in pubsub\n", e);
    }

    if (publisher != null) {
      // When finished with the publisher, shutdown to free up resources.
      publisher.shutdown();
      try {
        publisher.awaitTermination(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        log.error("Error occurred while terminating pubsub publisher\n", e);
      }
    }
  }
}
