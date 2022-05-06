/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NGEmbeddedUserDTO {
  private String name;
  private String email;

  public static NGEmbeddedUserDTO fromEmbeddedUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return NGEmbeddedUserDTO.builder().name(user.getName()).email(user.getEmail()).build();
  }
}
