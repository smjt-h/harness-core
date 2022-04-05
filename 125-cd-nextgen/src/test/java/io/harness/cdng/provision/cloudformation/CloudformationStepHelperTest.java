/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.rule.OwnerRule.TMACARI;

import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepHelper;

import software.wings.sm.states.provision.S3UriParser;

import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class CloudformationStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
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
  public void testRenderValue() {
    Ambiance ambiance = getAmbiance();
    String expression = "expression";
    cloudformationStepHelper.renderValue(ambiance, expression);
    verify(engineExpressionService).renderExpression(ambiance, expression);
  }

  public Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "account1");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org1");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project1");
    return Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
  }
}
