/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.rule;

import io.harness.beans.EmbeddedUser;
import io.harness.persistence.UserProvider;

import java.util.Optional;
import lombok.Setter;

public class TestUserProvider implements UserProvider {
  public static final TestUserProvider testUserProvider = new TestUserProvider();

  @Setter private EmbeddedUser activeUser;

  @Override
  public EmbeddedUser activeUser() {
    return activeUser;
  }

  @Override
  public Optional<EmbeddedUser> getCurrentAuditor() {
    return Optional.of(activeUser);
  }
}
