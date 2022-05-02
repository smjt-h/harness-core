/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.manifest.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ManifestStoreType.S3)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("s3Store")
@RecasterAlias("io.harness.cdng.manifest.yaml.S3StoreConfig")
public class S3StoreConfig implements StoreConfig, Visitable, WithConnectorRef {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> connectorRef;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> bucketName;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> region;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> folderPath;

  @Override
  public String getKind() {
    return ManifestStoreType.S3;
  }

  @Override
  public ParameterField<String> getConnectorReference() {
    return connectorRef;
  }

  public StoreConfig cloneInternal() {
    return S3StoreConfig.builder()
        .connectorRef(connectorRef)
        .bucketName(bucketName)
        .region(region)
        .folderPath(folderPath)
        .build();
  }

  @Override
  public StoreConfig applyOverrides(StoreConfig overrideConfig) {
    S3StoreConfig s3StoreConfig = (S3StoreConfig) overrideConfig;
    S3StoreConfig resultantS3Store = this;
    if (!ParameterField.isNull(s3StoreConfig.getConnectorRef())) {
      resultantS3Store = resultantS3Store.withConnectorRef(s3StoreConfig.getConnectorRef());
    }

    if (!ParameterField.isNull(s3StoreConfig.getBucketName())) {
      resultantS3Store = resultantS3Store.withBucketName(s3StoreConfig.getBucketName());
    }

    if (!ParameterField.isNull(s3StoreConfig.getRegion())) {
      resultantS3Store = resultantS3Store.withRegion(s3StoreConfig.getRegion());
    }

    if (!ParameterField.isNull(s3StoreConfig.getFolderPath())) {
      resultantS3Store = resultantS3Store.withFolderPath(s3StoreConfig.getFolderPath());
    }

    return resultantS3Store;
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
