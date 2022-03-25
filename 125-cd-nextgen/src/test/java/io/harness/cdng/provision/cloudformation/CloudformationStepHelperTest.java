/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.cdng.provision.cloudformation.CloudformationTemplateFileTypes.Remote;
import static io.harness.rule.OwnerRule.TMACARI;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.GitLabStore;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;

import software.wings.sm.states.provision.S3UriParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class CloudformationStepHelperTest extends CategoryTest {
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private K8sStepHelper k8sStepHelper;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private CDStepHelper cdStepHelper;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Mock private KryoSerializer kryoSerializer;
  @Mock private StepHelper stepHelper;
  @Mock private S3UriParser s3UriParser;
  @Mock private CloudformationStepExecutor cloudformationStepExecutor;
  @InjectMocks private final CloudformationStepHelper cloudformationStepHelper = new CloudformationStepHelper();

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testShouldGetGitFetchFileTaskChainResponse() {
    RemoteCloudformationTemplateFileSpec cloudformationTemplateFileSpec = new RemoteCloudformationTemplateFileSpec();
    cloudformationTemplateFileSpec.setStore(
        StoreConfigWrapper.builder()
            .spec(GitLabStore.builder()
                      .branch(ParameterField.createValueField("master"))
                      .gitFetchType(FetchType.BRANCH)
                      .connectorRef(ParameterField.createValueField("cloudformation"))
                      .paths(ParameterField.createValueField(Collections.singletonList("template.yaml")))
                      .build())
            .type(StoreConfigType.GITLAB)
            .build());
    StepElementParameters stepElementParameters =
        StepElementParameters.builder()
            .spec(CloudformationCreateStackStepParameters.infoBuilder()
                      .configuration(CloudformationCreateStackStepConfiguration.builder()
                                         .awsConnectorRef(ParameterField.createValueField("awsConnectorRef"))
                                         .templateFile(CloudformationTemplateFile.builder()
                                                           .type(Remote)
                                                           .spec(cloudformationTemplateFileSpec)
                                                           .build())
                                         .build())
                      .build())
            .build();
    cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);
  }

  public Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "account1");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org1");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project1");

    return Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
  }
}
