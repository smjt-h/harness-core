/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.buildtriggers.helpers.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.polling.contracts.ArtifactoryRegistryPayload;
import io.harness.polling.contracts.DockerHubPayload;
import io.harness.polling.contracts.PollingItem;
import io.harness.polling.contracts.PollingPayloadData;
import io.harness.polling.contracts.Type;
import lombok.AllArgsConstructor;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject}))
@OwnedBy(PIPELINE)
public class ArtifactoryRegistryPollingItemGenerator implements PollingItemGenerator {
    @Inject
    BuildTriggerHelper buildTriggerHelper;

    @Override
    public PollingItem generatePollingItem(BuildTriggerOpsData buildTriggerOpsData) {
        NGTriggerEntity ngTriggerEntity = buildTriggerOpsData.getTriggerDetails().getNgTriggerEntity();
        PollingItem.Builder builder = getBaseInitializedPollingItem(ngTriggerEntity);
        String connectorRef = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.connectorRef");
        String repository = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.repository");
        String artifactDirectory = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.artifactDirectory");
        String repositoryFormat = buildTriggerHelper.validateAndFetchFromJsonNode(buildTriggerOpsData, "spec.repositoryFormat");

        return builder
                .setPollingPayloadData(PollingPayloadData.newBuilder()
                        .setConnectorRef(connectorRef)
                        .setType(Type.ARTIFACTORY)
                        .setArtifactoryRegistryPayload(ArtifactoryRegistryPayload.newBuilder()
                                .setRepository(repository).setArtifactDirectory(artifactDirectory).setRepositoryFormat(repositoryFormat).build())
                        .build())
                .build();
    }
}
