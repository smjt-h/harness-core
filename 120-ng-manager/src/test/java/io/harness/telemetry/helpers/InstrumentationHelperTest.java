/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.telemetry.helpers;

import static io.harness.rule.OwnerRule.TEJAS;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ApiKeyPrincipal;
import io.harness.security.dto.ServiceAccountPrincipal;
import io.harness.security.dto.ServicePrincipal;
import io.harness.security.dto.UserPrincipal;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.PL)
@RunWith(PowerMockRunner.class)
@PrepareForTest(SecurityContextBuilder.class)
public class InstrumentationHelperTest {
  @InjectMocks InstrumentationHelper instrumentationHelper;
  private static final String EMAIL = "dummy@dummy";
  private static final String ACCOUNT_ID = "123";
  private static final String NAME = "dummy";

  @Before
  public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(SecurityContextBuilder.class);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testGetUserIdUserPrincipal() {
    Mockito.when(SecurityContextBuilder.getPrincipal())
        .thenReturn(new UserPrincipal("dummy", EMAIL, "dummy", ACCOUNT_ID));
    assertEquals(instrumentationHelper.getUserId(), EMAIL);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testGetUserIdServiceAccountPrincipal() {
    Mockito.when(SecurityContextBuilder.getPrincipal())
        .thenReturn(new ServiceAccountPrincipal("dummy", EMAIL, "dummy"));
    assertEquals(instrumentationHelper.getUserId(), EMAIL);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testGetUserIdServicePrincipal() {
    Mockito.when(SecurityContextBuilder.getPrincipal()).thenReturn(new ServicePrincipal(NAME));
    assertEquals(instrumentationHelper.getUserId(), NAME);
  }

  @Test
  @Owner(developers = TEJAS)
  @Category(UnitTests.class)
  public void testGetUserIdApiKeyPrincipal() {
    Mockito.when(SecurityContextBuilder.getPrincipal()).thenReturn(new ApiKeyPrincipal(NAME));
    assertEquals(instrumentationHelper.getUserId(), NAME);
  }
}
