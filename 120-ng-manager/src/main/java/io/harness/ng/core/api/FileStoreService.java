/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api;

import io.harness.ng.core.dto.filestore.FileDTO;

import javax.validation.Valid;

public interface FileStoreService {
  /**
   * Create file.
   *
   * @param fileDto the file DTO object
   * @return created file DTO object
   */
  FileDTO create(@Valid FileDTO fileDto);

  /**
   * Get a file.
   *
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the organization identifier
   * @param projectIdentifier the project identifier
   * @param identifier the file identifier
   * @return file
   */
  FileDTO get(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  /**
   * Update file.
   *
   * @param fileDto the file DTO
   * @return updated file DTO
   */
  FileDTO update(@Valid FileDTO fileDto);

  /**
   * Delete file.
   *
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the organization identifier
   * @param projectIdentifier the project identifier
   * @param identifier the file identifier
   * @return whether file is successfully deleted
   */
  boolean delete(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
