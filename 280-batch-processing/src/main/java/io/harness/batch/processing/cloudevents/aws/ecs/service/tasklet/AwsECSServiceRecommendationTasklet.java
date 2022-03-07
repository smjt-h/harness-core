/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import io.harness.batch.processing.billing.service.UtilizationDataWithTime;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.util.ServiceIdAndClusterId;
import io.harness.ccm.commons.beans.JobConstants;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.newCpuHistogramV2;

@Slf4j
@Singleton
public class AwsECSServiceRecommendationTasklet implements Tasklet {
  @Autowired private CEClusterDao ceClusterDao;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;

  private static final int BATCH_SIZE = 20;
  private static final int AVG_UTILIZATION_WEIGHT = 2;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    final JobConstants jobConstants = CCMJobConstants.fromContext(chunkContext);
    String accountId = jobConstants.getAccountId();
    Instant startTime = Instant.ofEpochMilli(jobConstants.getJobStartTime());
    Instant endTime = Instant.ofEpochMilli(jobConstants.getJobEndTime());
    List<String> ceClusters = ceClusterDao.getCEClusterIds(accountId);
    if (CollectionUtils.isEmpty(ceClusters)) {
      return null;
    }
    for (List<String> ceClustersPartition: Lists.partition(ceClusters, BATCH_SIZE)) {
      Map<ServiceIdAndClusterId, List<UtilizationDataWithTime>> utilMap =
          utilizationDataService.getUtilizationDataForECSClusters(accountId, ceClustersPartition,
              startTime.toString(), endTime.toString());

    }
    return null;
  }

  private HistogramCheckpoint histogramCheckpointFromUtilizationData(List<UtilizationDataWithTime> utilizationForDay) {
    Histogram histogram = histogramFromUtilizationData(utilizationForDay);
    return histogram.saveToCheckpoint();
  }

  private Histogram histogramFromUtilizationData(List<UtilizationDataWithTime> utilizationForDay) {
    Histogram histogram = newCpuHistogramV2();
    for (UtilizationDataWithTime utilizationForHour: utilizationForDay) {
      histogram.addSample(utilizationForHour.getUtilizationData().getAvgCpuUtilization(), 2,
          utilizationForHour.getStartTime());
      histogram.addSample(utilizationForHour.getUtilizationData().getMaxCpuUtilization(), 1,
          utilizationForHour.getStartTime());
    }
    return histogram;
  }
}
