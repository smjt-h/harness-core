/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.mappers.services;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.services.EnvironmentGroupServiceHelper;
import io.harness.rule.Owner;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.UserPrincipal;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

// TODO: move this method to EnvironmentGroupServiceHelperTest class. Currently have created a new test class for this
// powermock class since mocking of filterService class was failing in EnvironmentGroupServiceHelperTest
@RunWith(PowerMockRunner.class)
@PrepareForTest(SecurityContextBuilder.class)
public class EnvironmentGroupServiceHelperGetUserInfoTest extends CategoryTest {
  private String ACC_ID = "accId";

  private static final String EMAIL = "dummy@dummy";
  private static final String NAME = "dummy";

  @InjectMocks private EnvironmentGroupServiceHelper environmentGroupServiceHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(SecurityContextBuilder.class);
  }
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetUserInfo() {
    Mockito.when(SecurityContextBuilder.getPrincipal()).thenReturn(new UserPrincipal(NAME, EMAIL, "dummy", ACC_ID));
    EnvironmentGroupEntity entity = EnvironmentGroupEntity.builder().name("envGroup").identifier("envGroupId").build();
    entity = environmentGroupServiceHelper.updateEntityWithUserInfo(entity);
    assertThat(entity.getUserInfo()).isNotNull();
    assertThat(entity.getUserInfo().getId()).isEqualTo(NAME);
    assertThat(entity.getUserInfo().getEmail()).isEqualTo(EMAIL);
    assertThat(entity.getUserInfo().getName()).isEqualTo(NAME);
  }
}
