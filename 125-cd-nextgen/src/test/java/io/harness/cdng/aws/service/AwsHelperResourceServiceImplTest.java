/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.aws.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.NGONZALEZ;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(CDP)
public class AwsHelperResourceServiceImplTest extends CategoryTest {
  @Spy @InjectMocks private AwsHelperResourceServiceImpl service;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void getRegionTest() {
    Map<String, String> regions = service.getRegions();
    assertThat(regions).isNotEmpty();
    assertThat(regions.size()).isEqualTo(19);
    verify(service, times(1)).getMapRegionFromYaml(any());
  }
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void getMapRegioponFromYamlTest() {
    Map<String, String> regions = service.getRegions();
    assertThat(regions).isNotEmpty();
    assertThat(regions.size()).isEqualTo(19);
    service.getRegions();
    verify(service, times(1)).getMapRegionFromYaml(any());
  }
}
