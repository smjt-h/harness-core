/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.VerificationApplication;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.beans.job.TestVerificationJobDTO;
import io.harness.cvng.beans.job.VerificationJobDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.reflections.Reflections;
@OwnedBy(HarnessTeam.CV)
public class ProjectChangeEventMessageProcessorTest extends CvNextGenTestBase {
  @Inject private ProjectChangeEventMessageProcessor projectChangeEventMessageProcessor;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobService verificationJobService;
  @Inject private MetricPackService metricPackService;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws IllegalAccessException {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessDeleteAction() {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    CVConfig cvConfig1 = createCVConfig(accountId, orgIdentifier, "project1");
    CVConfig cvConfig2 = createCVConfig(accountId, orgIdentifier, "project2");
    cvConfigService.save(cvConfig1);
    cvConfigService.save(cvConfig2);
    VerificationJobDTO verificationJobDTO1 = createVerificationJobDTO(orgIdentifier, "project1");
    VerificationJobDTO verificationJobDTO2 = createVerificationJobDTO(orgIdentifier, "project2");
    verificationJobService.create(accountId, verificationJobDTO1);
    verificationJobService.create(accountId, verificationJobDTO2);
    projectChangeEventMessageProcessor.processDeleteAction(ProjectEntityChangeDTO.newBuilder()
                                                               .setAccountIdentifier(accountId)
                                                               .setOrgIdentifier(orgIdentifier)
                                                               .setIdentifier("project1")
                                                               .build());
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project1", verificationJobDTO1.getIdentifier()))
        .isNull();
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project2", verificationJobDTO2.getIdentifier()))
        .isNotNull();
    assertThat(cvConfigService.get(cvConfig1.getUuid())).isNull();
    assertThat(cvConfigService.get(cvConfig2.getUuid())).isNotNull();

    // For every message processing, idemptotency is assumed - Redelivery of a message produces the same result and
    // there are no side effects
    CVConfig retrievedCVConfig1 = cvConfigService.get(cvConfig1.getUuid());
    CVConfig retrievedCVConfig2 = cvConfigService.get(cvConfig2.getUuid());

    projectChangeEventMessageProcessor.processDeleteAction(ProjectEntityChangeDTO.newBuilder()
                                                               .setAccountIdentifier(accountId)
                                                               .setOrgIdentifier(orgIdentifier)
                                                               .setIdentifier("project1")
                                                               .build());
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project1", verificationJobDTO1.getIdentifier()))
        .isNull();
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project2", verificationJobDTO2.getIdentifier()))
        .isNotNull();
    assertThat(retrievedCVConfig1).isNull();
    assertThat(retrievedCVConfig2).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testProcessDeleteActionWithoutProjectIdentifier() {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    CVConfig cvConfig1 = createCVConfig(accountId, orgIdentifier, "project1");
    CVConfig cvConfig2 = createCVConfig(accountId, orgIdentifier, "project2");
    cvConfigService.save(cvConfig1);
    cvConfigService.save(cvConfig2);
    VerificationJobDTO verificationJobDTO1 = createVerificationJobDTO(orgIdentifier, "project1");
    VerificationJobDTO verificationJobDTO2 = createVerificationJobDTO(orgIdentifier, "project2");
    verificationJobService.create(accountId, verificationJobDTO1);
    verificationJobService.create(accountId, verificationJobDTO2);
    projectChangeEventMessageProcessor.processDeleteAction(
        ProjectEntityChangeDTO.newBuilder().setAccountIdentifier(accountId).setOrgIdentifier(orgIdentifier).build());

    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project1", verificationJobDTO1.getIdentifier()))
        .isNotNull();
    assertThat(verificationJobService.getVerificationJobDTO(
                   accountId, orgIdentifier, "project2", verificationJobDTO2.getIdentifier()))
        .isNotNull();
    assertThat(cvConfigService.get(cvConfig1.getUuid())).isNotNull();
    assertThat(cvConfigService.get(cvConfig2.getUuid())).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessCreateAction() throws IllegalAccessException {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    String projectIdentifier = generateUuid();
    projectChangeEventMessageProcessor.processCreateAction(ProjectEntityChangeDTO.newBuilder()
                                                               .setAccountIdentifier(accountId)
                                                               .setOrgIdentifier(orgIdentifier)
                                                               .setIdentifier(projectIdentifier)
                                                               .build());

    List<MetricPack> metricPacks =
        metricPackService.getMetricPacks(accountId, orgIdentifier, projectIdentifier, DataSourceType.APP_DYNAMICS);
    assertThat(metricPacks).isNotEmpty();

    List<TimeSeriesThreshold> metricPackThresholds = metricPackService.getMetricPackThresholds(
        accountId, orgIdentifier, projectIdentifier, metricPacks.get(0).getIdentifier(), DataSourceType.APP_DYNAMICS);

    assertThat(metricPackThresholds).isNotEmpty();
    assertThat(metricPackThresholds).size().isEqualTo(2);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessDeleteAction_entitiesList() {
    Set<Class<? extends PersistentEntity>> entitiesWithVerificationTaskId = new HashSet<>();
    entitiesWithVerificationTaskId.addAll(ProjectChangeEventMessageProcessor.ENTITIES_MAP.keySet());
    Reflections reflections = new Reflections(VerificationApplication.class.getPackage().getName());
    Set<Class<? extends PersistentEntity>> withProjectIdentifier = new HashSet<>();
    reflections.getSubTypesOf(PersistentEntity.class).forEach(entity -> {
      if (doesClassContainField(entity, "accountId") && doesClassContainField(entity, "orgIdentifier")
          && doesClassContainField(entity, "projectIdentifier")) {
        withProjectIdentifier.add(entity);
      }
    });
    assertThat(entitiesWithVerificationTaskId)
        .isEqualTo(withProjectIdentifier)
        .withFailMessage("Entities with projectIdentifier found which is not added to ENTITIES_MAP");
  }

  private boolean doesClassContainField(Class<?> clazz, String fieldName) {
    return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
  }

  private CVConfig createCVConfig(String accountId, String orgIdentifier, String projectIdentifier) {
    return builderFactory.splunkCVConfigBuilder()
        .accountId(accountId)
        .projectIdentifier(projectIdentifier)
        .orgIdentifier(orgIdentifier)
        .build();
  }

  private VerificationJobDTO createVerificationJobDTO(String orgIdentifier, String projectIdentifier) {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(generateUuid());
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setMonitoringSources(Arrays.asList(generateUuid()));
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    testVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    return testVerificationJobDTO;
  }
}
