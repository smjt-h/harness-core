package io.harness.connector.entities.embedded.pdcconnector;

import io.harness.connector.entities.Connector;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "PhysicalDataCenterConnectorKeys")
@Entity(value = "connectors", noClassnameStored = true)
@Persistent
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.connector.entities.embedded.pdcconnector.PhysicalDataCenterConnector")
public class PhysicalDataCenterConnector extends Connector {
  String hostNames;
  String sshKeyRef;
}
