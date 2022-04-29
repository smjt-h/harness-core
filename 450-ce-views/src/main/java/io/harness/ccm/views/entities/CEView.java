/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.entities;

import io.harness.annotation.StoreIn;
import io.harness.beans.EmbeddedUser;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@StoreIn(DbAliases.CENG)
@FieldNameConstants(innerTypeName = "CEViewKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(value = "ceView", noClassnameStored = true)
@Schema(description = "This object will contain the complete definition of a Cloud Cost Perspective")
public final class CEView implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess,
                                     CreatedByAware, UpdatedByAware {
  @Id String uuid;
  @Size(min = 1, max = 80, message = "for view must be between 1 and 80 characters long") @NotBlank String name;
  String accountId;
  @NotBlank String viewVersion;

  ViewTimeRange viewTimeRange;
  List<ViewRule> viewRules;
  List<ViewFieldIdentifier> dataSources;
  ViewVisualization viewVisualization;
  ViewType viewType = ViewType.CUSTOMER;

  ViewState viewState = ViewState.DRAFT;

  double totalCost;
  long createdAt;
  long lastUpdatedAt;
  private EmbeddedUser createdBy;
  private EmbeddedUser lastUpdatedBy;
}
