/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class PhysicalDataCenterUtilsTest {
  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldExtractPortFromEmptyHost() {
    Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("");
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldExtractPortFromHost() {
    Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("localhost:8080");
    assertThat(result.get()).isEqualTo(8080);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldExtractPortFromHostNotANumber() {
    Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("local:host");
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testExtractHostnameFromHost() {
    Optional<String> hostname = PhysicalDataCenterUtils.extractHostnameFromHost("localhost:22");
    assertThat(hostname.isPresent()).isTrue();
    assertThat(hostname.get()).isEqualTo("localhost");

    hostname = PhysicalDataCenterUtils.extractHostnameFromHost("127.0.0.1:22");
    assertThat(hostname.isPresent()).isTrue();
    assertThat(hostname.get()).isEqualTo("127.0.0.1");
  }
}
