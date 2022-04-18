/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.core.utils.DateTimeUtils;

import com.google.common.base.Preconditions;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public enum DurationDTO {
  ONE_HOUR(Duration.ofHours(1)),
  FOUR_HOURS(Duration.ofHours(4)),
  TWENTY_FOUR_HOURS(Duration.ofDays(1)),
  THREE_DAYS(Duration.ofDays(3)),
  SEVEN_DAYS(Duration.ofDays(7)),
  THIRTY_DAYS(Duration.ofDays(30));

  private static Clock clock = Clock.systemUTC(); // Use Injection here.

  private Duration duration;

  DurationDTO(Duration duration) {
    this.duration = duration;
  }
  public Duration getDuration() {
    return duration;
  }

  public static Pair<Instant, Instant> getCustomTimeRange(Instant startTime, Instant endTime, DurationDTO durationDTO) {
    if (!Objects.isNull(durationDTO)) {
      endTime = DateTimeUtils.roundDownTo1MinBoundary(clock.instant());
      switch (durationDTO) {
        case ONE_HOUR:
          startTime = DateTimeUtils.roundDownToHourBoundary(clock.instant(), 1);
          break;
        case TWENTY_FOUR_HOURS:
          startTime = DateTimeUtils.roundDownToDayBoundary(clock.instant(), 1);
          break;
        case SEVEN_DAYS:
          startTime = DateTimeUtils.roundDownToOneWeekBoundary(clock.instant());
          break;
        case THIRTY_DAYS:
          startTime = DateTimeUtils.roundDownToOneMonthBoundary(clock.instant());
          break;
        default:
          startTime = null;
      }
    } else {
      if (startTime.equals(Instant.ofEpochMilli(0)) || endTime.equals(Instant.ofEpochMilli(0))) {
        startTime = null;
        endTime = null;
      } else {
        startTime = DateTimeUtils.roundDownTo1MinBoundary(startTime);
        endTime = DateTimeUtils.roundDownTo1MinBoundary(endTime);
        Preconditions.checkArgument(Duration.between(startTime, endTime).toHours() >= 1,
            "Custom duration must be greater than or equal one hour.");
      }
    }
    return new ImmutablePair<>(startTime, endTime);
  }
}
