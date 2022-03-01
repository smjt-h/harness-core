/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;
import static io.harness.debezium.DebeziumOffset.OFFSET_COLLECTION;
import static io.harness.ng.DbAliases.PMS;

import io.harness.annotation.StoreIn;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@Document(OFFSET_COLLECTION)
@Entity(value = OFFSET_COLLECTION, noClassnameStored = true)
@FieldNameConstants(innerTypeName = "keys")
@StoreIn(PMS)
@TypeAlias(OFFSET_COLLECTION)
public class DebeziumOffset implements PersistentEntity {
  public static final String OFFSET_COLLECTION = "debeziumOffset";

  @Id @org.mongodb.morphia.annotations.Id private String id;
  private byte[] key;
  private byte[] value;
  @FdIndex @CreatedDate private long createdAt;
}
