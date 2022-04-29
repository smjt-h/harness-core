/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ARPITJ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.core.beans.sidekick.RetryChangeSourceHandleDeleteSideKickData;
import io.harness.cvng.core.entities.SideKick;
import io.harness.cvng.core.entities.SideKick.SidekickKeys;
import io.harness.cvng.core.entities.SideKick.Status;
import io.harness.cvng.core.entities.SideKick.Type;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.cvng.core.services.api.SideKickExecutor;
import io.harness.cvng.core.services.api.SideKickExecutor.RetryData;
import io.harness.cvng.core.services.impl.sidekickexecutors.RetryChangeSourceHandleDeleteSideKickExecutor;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mongodb.morphia.query.Sort;

@Slf4j
public class SideKickServiceImplTest extends CvNextGenTestBase {
  @Inject SideKickServiceImpl sideKickService;
  @Inject HPersistence hPersistence;
  Map<SideKick.Type, SideKickExecutor> typeSideKickExecutorMap;

  private BuilderFactory builderFactory;

  RetryChangeSourceHandleDeleteSideKickExecutor retryChangeSourceHandleDeleteSideKickExecutor;

  @SneakyThrows
  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    typeSideKickExecutorMap = new HashMap<>();
    retryChangeSourceHandleDeleteSideKickExecutor = Mockito.mock(RetryChangeSourceHandleDeleteSideKickExecutor.class);
    typeSideKickExecutorMap.put(Type.RETRY_CHANGE_SOURCE_HANDLE_DELETE, retryChangeSourceHandleDeleteSideKickExecutor);
    FieldUtils.writeField(sideKickService, "typeSideKickExecutorMap", typeSideKickExecutorMap, true);
    FieldUtils.writeField(sideKickService, "clock", Clock.systemDefaultZone(), true);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void test_schedule() {
    RetryChangeSourceHandleDeleteSideKickData sideKickData = createRetryChangeSourceHandleDeleteSideKick();
    sideKickService.schedule(sideKickData, Instant.now().plusSeconds(300));
    SideKick sideKick = hPersistence.createQuery(SideKick.class).order(Sort.descending(SidekickKeys.createdAt)).get();
    assertThat(sideKick.getSideKickData()).isEqualTo(sideKickData);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void test_ProcessNext() {
    RetryChangeSourceHandleDeleteSideKickData sideKickData = createRetryChangeSourceHandleDeleteSideKick();
    sideKickService.schedule(sideKickData, Instant.now());
    doNothing()
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .execute(any(RetryChangeSourceHandleDeleteSideKickData.class));
    sideKickService.processNext();
    SideKick sideKick = hPersistence.createQuery(SideKick.class).order(Sort.descending(SidekickKeys.createdAt)).get();
    assertThat(sideKick.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void test_ProcessNextStepFailure() {
    RetryChangeSourceHandleDeleteSideKickData sideKickData = createRetryChangeSourceHandleDeleteSideKick();
    sideKickService.schedule(sideKickData, Instant.now());
    doThrow(SocketTimeoutException.class)
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .execute(any(RetryChangeSourceHandleDeleteSideKickData.class));

    doReturn(RetryData.builder().shouldRetry(true).nextRetryTime(Instant.now().plusSeconds(300)).build())
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .shouldRetry(0);

    sideKickService.processNext();
    SideKick sideKick = hPersistence.createQuery(SideKick.class).order(Sort.descending(SidekickKeys.createdAt)).get();

    assertThat(sideKick.getStatus()).isEqualTo(Status.QUEUED);
    assertThat(sideKick.getRetryCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void test_ProcessNextFailure() {
    RetryChangeSourceHandleDeleteSideKickData sideKickData = createRetryChangeSourceHandleDeleteSideKick();
    sideKickService.schedule(sideKickData, Instant.now());
    doThrow(SocketTimeoutException.class)
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .execute(any(RetryChangeSourceHandleDeleteSideKickData.class));

    doReturn(RetryData.builder().shouldRetry(true).nextRetryTime(Instant.now()).build())
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .shouldRetry(0);
    doReturn(RetryData.builder().shouldRetry(true).nextRetryTime(Instant.now()).build())
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .shouldRetry(1);
    doReturn(RetryData.builder().shouldRetry(false).nextRetryTime(Instant.now()).build())
        .when(retryChangeSourceHandleDeleteSideKickExecutor)
        .shouldRetry(2);

    sideKickService.processNext();
    SideKick sideKick = hPersistence.createQuery(SideKick.class).order(Sort.descending(SidekickKeys.createdAt)).get();

    assertThat(sideKick.getStatus()).isEqualTo(Status.FAILED);
  }

  private RetryChangeSourceHandleDeleteSideKickData createRetryChangeSourceHandleDeleteSideKick() {
    PagerDutyChangeSource changeSource = builderFactory.getPagerDutyChangeSourceBuilder().build();
    return RetryChangeSourceHandleDeleteSideKickData.builder().changeSource(changeSource).build();
  }
}
