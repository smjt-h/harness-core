/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.recommendation;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.EPSILON;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.CPU;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.MEMORY;

import io.harness.ccm.commons.dao.recommendation.ECSRecommendationDAO;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation;
import io.harness.ccm.commons.utils.StrippedHistogram;
import io.harness.ccm.graphql.dto.recommendation.ContainerHistogramDTO.HistogramExp;
import io.harness.ccm.graphql.dto.recommendation.ECSRecommendationDTO;
import io.harness.histogram.Histogram;
import io.harness.histogram.HistogramCheckpoint;
import io.harness.histogram.HistogramImpl;
import io.harness.histogram.LinearHistogramOptions;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.custom.Quantity;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;

@Singleton
public class ECSRecommendationService {
  private static final int NUMBER_OF_BUCKETS = 1000;
  @Inject private ECSRecommendationDAO ecsRecommendationDAO;

  @Nullable
  public ECSRecommendationDTO getECSRecommendationById(@NonNull final String accountIdentifier, String id,
      @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime) {
    final Optional<ECSServiceRecommendation> ecsRecommendation =
        ecsRecommendationDAO.fetchECSRecommendationById(accountIdentifier, id);

    if (!ecsRecommendation.isPresent()) {
      return ECSRecommendationDTO.builder().build();
    }

    ECSServiceRecommendation recommendation = ecsRecommendation.get();

    final List<ECSPartialRecommendationHistogram> histogramList =
        ecsRecommendationDAO.fetchPartialRecommendationHistograms(accountIdentifier, recommendation.getClusterId(),
            recommendation.getServiceArn(), startTime.toInstant(), endTime.toInstant());
    return mergeHistogram(histogramList, recommendation);
  }

  @NonNull
  private ECSRecommendationDTO mergeHistogram(
      final List<ECSPartialRecommendationHistogram> histogramList, ECSServiceRecommendation recommendation) {
    long memoryMb = memoryMbFromReadableFormat(recommendation.getCurrentResourceRequirements().get(MEMORY));
    long cpuUnits = cpuUnitsFromReadableFormat(recommendation.getCurrentResourceRequirements().get(CPU));
    Histogram memoryHistogram = newHistogram(memoryMb);
    Histogram cpuHistogram = newHistogram(cpuUnits);

    for (ECSPartialRecommendationHistogram partialHistogram : histogramList) {
      Histogram partialMemoryHistogram = newHistogram(memoryMb);
      partialMemoryHistogram.loadFromCheckPoint(partialHistogram.getMemoryHistogram());
      memoryHistogram.merge(partialMemoryHistogram);
      Histogram partialCpuHistogram = newHistogram(cpuUnits);
      partialCpuHistogram.loadFromCheckPoint(partialHistogram.getCpuHistogram());
      cpuHistogram.merge(partialCpuHistogram);
    }

    HistogramCheckpoint memoryHistogramCp = memoryHistogram.saveToCheckpoint();
    HistogramCheckpoint cpuHistogramCp = cpuHistogram.saveToCheckpoint();
    StrippedHistogram memStripped = StrippedHistogram.fromCheckpoint(memoryHistogramCp, NUMBER_OF_BUCKETS);
    StrippedHistogram cpuStripped = StrippedHistogram.fromCheckpoint(cpuHistogramCp, NUMBER_OF_BUCKETS);

    return ECSRecommendationDTO.builder()
        .id(recommendation.getUuid())
        .clusterName(recommendation.getClusterName())
        .serviceArn(recommendation.getServiceArn())
        .serviceName(recommendation.getServiceName())
        .current(recommendation.getCurrentResourceRequirements())
        .percentileBased(recommendation.getPercentileBasedResourceRecommendation())
        .lastDayCost(recommendation.getLastDayCost())
        .memoryHistogram(HistogramExp.builder()
                             .numBuckets(memStripped.getNumBuckets())
                             .minBucket(memStripped.getMinBucket())
                             .maxBucket(memStripped.getMaxBucket())
                             .bucketWeights(memStripped.getBucketWeights())
                             .precomputed(getPrecomputedPercentiles(memoryHistogram))
                             .totalWeight(memoryHistogramCp.getTotalWeight())
                             .build())
        .cpuHistogram(HistogramExp.builder()
                          .numBuckets(cpuStripped.getNumBuckets())
                          .minBucket(cpuStripped.getMinBucket())
                          .maxBucket(cpuStripped.getMaxBucket())
                          .bucketWeights(cpuStripped.getBucketWeights())
                          .precomputed(getPrecomputedPercentiles(cpuHistogram))
                          .totalWeight(cpuHistogramCp.getTotalWeight())
                          .build())
        .build();
  }

  private static Histogram newHistogram(long maxUnits) {
    // Histogram will have 1000 buckets
    return new HistogramImpl(new LinearHistogramOptions(maxUnits, maxUnits / (double) NUMBER_OF_BUCKETS, EPSILON));
  }

  private double[] getPrecomputedPercentiles(Histogram histogram) {
    double[] result = new double[101];
    for (int p = 1; p <= 100; p++) {
      result[p] = histogram.getPercentile(p / 100.0);
    }
    return result;
  }

  private static long cpuUnitsFromReadableFormat(String cpu) {
    return getAmountFromReadableFormat(cpu).longValue();
  }

  private static long memoryMbFromReadableFormat(String memory) {
    return getAmountFromReadableFormat(memory).scaleByPowerOfTen(-6).longValue();
  }

  private static BigDecimal getAmountFromReadableFormat(String s) {
    return Quantity.fromString(s).getNumber();
  }
}
