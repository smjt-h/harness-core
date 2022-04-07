/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.VLAD;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.api.impl.FileStoreServiceImpl;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CDP)
public class FileStoreResourceTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private static final String ORG = "org";
  private static final String PROJECT = "project";
  private static final String IDENTIFIER = "testFile";

  @Mock private FileStoreServiceImpl fileStoreService;

  private FileStoreResource fileStoreResource;

  @Before
  public void setup() {
    initMocks(this);
    fileStoreResource = new FileStoreResource(fileStoreService);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    fileStoreResource.delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    verify(fileStoreService).delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
  }
}
