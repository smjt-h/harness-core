/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.buildtriggers.helper.generator.artifacts;

import com.google.common.io.Resources;
import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.dto.PollingConfig;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.buildtriggers.helper.generator.PollingItemGeneratorTestHelper;
import io.harness.ngtriggers.buildtriggers.helpers.BuildTriggerHelper;
import io.harness.ngtriggers.buildtriggers.helpers.dtos.BuildTriggerOpsData;
import io.harness.ngtriggers.buildtriggers.helpers.generator.ArtifactoryRegistryPollingItemGenerator;
import io.harness.ngtriggers.buildtriggers.helpers.generator.DockerRegistryPollingItemGenerator;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.polling.contracts.PollingItem;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

@OwnedBy(PIPELINE)
public class ArtifactoryRegistryPollingItemGeneratorTest extends CategoryTest {
    BuildTriggerHelper buildTriggerHelper = new BuildTriggerHelper(null);
    @InjectMocks
    NGTriggerElementMapper ngTriggerElementMapper;
    ArtifactoryRegistryPollingItemGenerator artifactoryRegistryPollingItemGenerator =
            new ArtifactoryRegistryPollingItemGenerator(buildTriggerHelper);
    String artifactory_pipeline_artifact_snippet_runtime_all;
    String artifactory_pipeline_artifact_snippet_runtime_artifactPathonly;
    String ngTriggerYaml_artifact_artifactory;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        ClassLoader classLoader = getClass().getClassLoader();
        artifactory_pipeline_artifact_snippet_runtime_all = Resources.toString(
                Objects.requireNonNull(classLoader.getResource("artifactory_pipeline_artifact_snippet_runtime_all.yaml")),
                StandardCharsets.UTF_8);
        artifactory_pipeline_artifact_snippet_runtime_artifactPathonly = Resources.toString(
                Objects.requireNonNull(
                        classLoader.getResource("artifactory_pipeline_artifact_snippet_runtime_artifactpathonly.yaml")),
                StandardCharsets.UTF_8);

        ngTriggerYaml_artifact_artifactory =
                Resources.toString(Objects.requireNonNull(classLoader.getResource("ng-trigger-artifact-artifactory.yaml")),
                        StandardCharsets.UTF_8);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testArtifactoryPollingItemGeneration_pipelineContainsFixedValuesExceptArtifactoryPath() throws Exception {
        TriggerDetails triggerDetails =
                ngTriggerElementMapper.toTriggerDetails("acc", "org", "proj", ngTriggerYaml_artifact_artifactory);
        triggerDetails.getNgTriggerEntity().getMetadata().getBuildMetadata().setPollingConfig(
                PollingConfig.builder().signature("sig1").build());
        BuildTriggerOpsData buildTriggerOpsData = buildTriggerHelper.generateBuildTriggerOpsDataForArtifact(
                triggerDetails, artifactory_pipeline_artifact_snippet_runtime_artifactPathonly);
        PollingItem pollingItem = artifactoryRegistryPollingItemGenerator.generatePollingItem(buildTriggerOpsData);

        PollingItemGeneratorTestHelper.baseAssert(pollingItem, io.harness.polling.contracts.Category.ARTIFACT);

        assertThat(pollingItem.getPollingPayloadData()).isNotNull();
        assertThat(pollingItem.getPollingPayloadData().getConnectorRef()).isEqualTo("conn");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload()).isNotNull();
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getArtifactDirectory()).isEqualTo("artifactstest");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getRepository()).isEqualTo("automation-repo-do-not-delete");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getRepositoryFormat()).isEqualTo("generic");

        // As All data is already prepared, Testing buildTriggerHelper.validateBuildType
        PollingItemGeneratorTestHelper.validateBuildType(buildTriggerOpsData, buildTriggerHelper);
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testArtifactoryPollingItemGeneration_pipelineContainsAllRuntimeInputs() throws Exception {
        TriggerDetails triggerDetails =
                ngTriggerElementMapper.toTriggerDetails("acc", "org", "proj", ngTriggerYaml_artifact_artifactory);
        triggerDetails.getNgTriggerEntity().getMetadata().getBuildMetadata().setPollingConfig(
                PollingConfig.builder().signature("sig1").build());
        BuildTriggerOpsData buildTriggerOpsData = buildTriggerHelper.generateBuildTriggerOpsDataForArtifact(
                triggerDetails, artifactory_pipeline_artifact_snippet_runtime_all);
        PollingItem pollingItem = artifactoryRegistryPollingItemGenerator.generatePollingItem(buildTriggerOpsData);

        PollingItemGeneratorTestHelper.baseAssert(pollingItem, io.harness.polling.contracts.Category.ARTIFACT);

        assertThat(pollingItem.getPollingPayloadData()).isNotNull();
        assertThat(pollingItem.getPollingPayloadData().getConnectorRef()).isEqualTo("account.conn");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload()).isNotNull();
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getArtifactDirectory()).isEqualTo("artifactstest");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getRepository()).isEqualTo("automation-repo-do-not-delete");
        assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload().getRepositoryFormat()).isEqualTo("generic");assertThat(pollingItem.getPollingPayloadData().getArtifactoryRegistryPayload()).isNotNull();


        // As All data is already prepared, Testing buildTriggerHelper.validateBuildType
        PollingItemGeneratorTestHelper.validateBuildType(buildTriggerOpsData, buildTriggerHelper);
    }
}
