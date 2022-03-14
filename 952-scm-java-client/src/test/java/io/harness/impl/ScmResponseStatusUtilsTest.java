/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.impl;

import static io.harness.rule.OwnerRule.SATYAM_GOEL;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class ScmResponseStatusUtilsTest extends CategoryTest {
  @Test(expected = WingsException.class)
  @Owner(developers = SATYAM_GOEL)
  @Category(UnitTests.class)
  public void checkScmResponseStatusAndThrowException_throwsError() {
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(422, "unable to push data");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = SATYAM_GOEL)
  @Category(UnitTests.class)
  public void checkScmResponseStatusAndThrowException_shouldNotThrowsError() {
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(0, "testMdg");
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(299, "testMdg");
  }
}
