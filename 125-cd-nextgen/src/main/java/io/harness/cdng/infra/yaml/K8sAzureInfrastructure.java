/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.infra.yaml;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.infra.beans.InfraMapping;
import io.harness.cdng.infra.beans.K8sAzureInfraMapping;
import io.harness.filters.ConnectorRefExtractorHelper;
import io.harness.filters.WithConnectorRef;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Wither;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(InfrastructureKind.KUBERNETES_AZURE)
@SimpleVisitorHelper(helperClass = ConnectorRefExtractorHelper.class)
@TypeAlias("k8sAzureInfrastructure")
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.infra.yaml.K8sAzureInfrastructure")
public class K8sAzureInfrastructure implements Infrastructure, Visitable, WithConnectorRef {
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> connectorRef;
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> namespace;
  @NotNull
  @NotEmpty
  @SkipAutoEvaluation
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> releaseName;
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> cluster;
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> subscriptionId;
  @NotNull
  @NotEmpty
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  @Wither
  ParameterField<String> resourceGroup;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public InfraMapping getInfraMapping() {
    return K8sAzureInfraMapping.builder()
        .gcpConnector(connectorRef.getValue())
        .namespace(namespace.getValue())
        .cluster(cluster.getValue())
        .subscription(cluster.getValue())
        .resourceGroup(cluster.getValue())
        .build();
  }

  @Override
  public ParameterField<String> getConnectorReference() {
    return connectorRef;
  }

  @Override
  public String[] getInfrastructureKeyValues() {
    return new String[] {connectorRef.getValue(), cluster.getValue(), namespace.getValue(), subscriptionId.getValue(),
        resourceGroup.getValue()};
  }

  @Override
  public String getKind() {
    return InfrastructureKind.KUBERNETES_AZURE;
  }

  @Override
  public Infrastructure applyOverrides(Infrastructure overrideConfig) {
    K8sAzureInfrastructure config = (K8sAzureInfrastructure) overrideConfig;
    K8sAzureInfrastructure resultantInfra = this;
    if (!ParameterField.isNull(config.getConnectorRef())) {
      resultantInfra = resultantInfra.withConnectorRef(config.getConnectorRef());
    }
    if (!ParameterField.isNull(config.getNamespace())) {
      resultantInfra = resultantInfra.withNamespace(config.getNamespace());
    }
    if (!ParameterField.isNull(config.getCluster())) {
      resultantInfra = resultantInfra.withCluster(config.getCluster());
    }
    if (!ParameterField.isNull(config.getReleaseName())) {
      resultantInfra = resultantInfra.withReleaseName(config.getReleaseName());
    }
    if (!ParameterField.isNull(config.getSubscriptionId())) {
      resultantInfra = resultantInfra.withSubscriptionId(config.getSubscriptionId());
    }
    if (!ParameterField.isNull(config.getResourceGroup())) {
      resultantInfra = resultantInfra.withResourceGroup(config.getResourceGroup());
    }
    return resultantInfra;
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();
    connectorRefMap.put(YAMLFieldNameConstants.CONNECTOR_REF, connectorRef);
    return connectorRefMap;
  }
}
