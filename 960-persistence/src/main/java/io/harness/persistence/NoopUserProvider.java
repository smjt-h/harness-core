/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.persistence;

import io.harness.beans.EmbeddedUser;

import java.util.Optional;

public class NoopUserProvider implements UserProvider {
  @Override
  public EmbeddedUser activeUser() {
    return null;
  }

  @Override
  public Optional<EmbeddedUser> getCurrentAuditor() {
    return Optional.empty();
  }
}
