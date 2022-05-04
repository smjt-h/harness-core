/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.tasklet;

import static io.harness.rule.OwnerRule.TRUNAPUSHPA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.billing.timeseries.service.impl.BillingDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.AwsECSServiceRecommendationTasklet;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.ClusterIdAndServiceArn;
import io.harness.batch.processing.dao.intfc.ECSServiceDao;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.beans.Resource;
import io.harness.ccm.commons.dao.recommendation.ECSRecommendationDAO;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;
import io.harness.testsupport.BaseTaskletTest;

import software.wings.graphql.datafetcher.ce.recommendation.entity.Cost;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AwsECSServiceRecommendationTaskletTest extends BaseTaskletTest {
  @Mock private CEClusterDao ceClusterDao;
  @Mock private ECSServiceDao ecsServiceDao;
  @Mock private UtilizationDataServiceImpl utilizationDataService;
  @Mock private ECSRecommendationDAO ecsRecommendationDAO;
  @Mock private BillingDataServiceImpl billingDataService;
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks private AwsECSServiceRecommendationTasklet tasklet;

  private static final String CLUSTER_NAME = "clusterName";
  private static final String CLUSTER_ID = "clusterId";
  private static final String SERVICE_ARN = "accountId/serviceName";
  private static final Double CPU_UNITS = 1024.0;
  private static final Double MEMORY_MB = 1024.0;

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testExecuteSuccess() throws Exception {
    assertThat(tasklet.execute(null, chunkContext)).isNull();
  }

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testExecuteFeatureFlagDisabled() throws Exception {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
    assertThat(tasklet.execute(null, chunkContext)).isNull();
  }

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testExecuteNoClusters() throws Exception {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    when(ceClusterDao.getClusterIdNameMapping(any())).thenReturn(Collections.emptyMap());
    assertThat(tasklet.execute(null, chunkContext)).isNull();
    verify(ceClusterDao, times(1)).getClusterIdNameMapping(any());
  }

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testExecuteNoUtilData() throws Exception {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    when(ceClusterDao.getClusterIdNameMapping(any())).thenReturn(Collections.singletonMap(CLUSTER_ID, CLUSTER_NAME));
    when(utilizationDataService.getUtilizationDataForECSClusters(any(), any(), any(), any()))
        .thenReturn(Collections.emptyMap());
    assertThat(tasklet.execute(null, chunkContext)).isNull();
    verify(ceClusterDao, times(1)).getClusterIdNameMapping(any());
    verify(utilizationDataService, times(1)).getUtilizationDataForECSClusters(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testNoServiceInfoPresent() throws Exception {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    when(ceClusterDao.getClusterIdNameMapping(any())).thenReturn(Collections.singletonMap(CLUSTER_ID, CLUSTER_NAME));
    when(utilizationDataService.getUtilizationDataForECSClusters(any(), any(), any(), any()))
        .thenReturn(
            Collections.singletonMap(new ClusterIdAndServiceArn(CLUSTER_ID, SERVICE_ARN), Collections.emptyList()));
    when(ecsServiceDao.fetchResourceForServices(any(), any())).thenReturn(Collections.emptyMap());
    assertThat(tasklet.execute(null, chunkContext)).isNull();
    verify(ceClusterDao, times(1)).getClusterIdNameMapping(any());
    verify(utilizationDataService, times(1)).getUtilizationDataForECSClusters(any(), any(), any(), any());
    verify(ecsServiceDao, times(1)).fetchResourceForServices(any(), any());
    verify(ecsRecommendationDAO, times(0)).savePartialRecommendation(any());
    verify(ecsRecommendationDAO, times(0)).saveRecommendation(any());
  }

  @Test
  @Owner(developers = TRUNAPUSHPA)
  @Category(UnitTests.class)
  public void testNoPartialHistograms() throws Exception {
    when(featureFlagService.isEnabled(any(), any())).thenReturn(true);
    when(ceClusterDao.getClusterIdNameMapping(any())).thenReturn(Collections.singletonMap(CLUSTER_ID, CLUSTER_NAME));
    when(utilizationDataService.getUtilizationDataForECSClusters(any(), any(), any(), any()))
        .thenReturn(
            Collections.singletonMap(new ClusterIdAndServiceArn(CLUSTER_ID, SERVICE_ARN), Collections.emptyList()));
    when(ecsServiceDao.fetchResourceForServices(any(), any()))
        .thenReturn(
            Collections.singletonMap(SERVICE_ARN, Resource.builder().cpuUnits(CPU_UNITS).memoryMb(MEMORY_MB).build()));
    when(ecsRecommendationDAO.fetchPartialRecommendationHistograms(any(), any(), any(), any(), any()))
        .thenReturn(new ArrayList<>());
    when(billingDataService.getECSServiceLastAvailableDayCost(any(), any(), any(), any())).thenReturn(cost());
    when(ecsRecommendationDAO.savePartialRecommendation(any()))
        .thenReturn(ECSPartialRecommendationHistogram.builder().build());
    when(ecsRecommendationDAO.saveRecommendation(any()))
        .thenReturn(ECSServiceRecommendation.builder().uuid("uuid").build());
    doNothing()
        .when(ecsRecommendationDAO)
        .upsertCeRecommendation(
            anyString(), anyString(), anyString(), anyString(), anyDouble(), anyDouble(), anyBoolean(), any());
    assertThat(tasklet.execute(null, chunkContext)).isNull();
    verify(ceClusterDao, times(1)).getClusterIdNameMapping(any());
    verify(utilizationDataService, times(1)).getUtilizationDataForECSClusters(any(), any(), any(), any());
    verify(ecsServiceDao, times(1)).fetchResourceForServices(any(), any());
    verify(ecsRecommendationDAO, times(1)).savePartialRecommendation(any());
    verify(ecsRecommendationDAO, times(1)).saveRecommendation(any());
    verify(ecsRecommendationDAO, times(1))
        .upsertCeRecommendation(
            anyString(), anyString(), anyString(), anyString(), anyDouble(), anyDouble(), anyBoolean(), any());
  }

  private Cost cost() {
    return Cost.builder().memory(new BigDecimal("0.1")).cpu(new BigDecimal("0.1")).build();
  }
}
