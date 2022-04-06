/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.entity;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "SecretVariableKeys")
@Entity(value = "variables", noClassnameStored = true)
@Persistent
@TypeAlias("io.harness.ng.core.variables.entity.SecretVariable")
public class SecretVariable extends Variable {
  String secretRef;
}
