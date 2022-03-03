/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.cloudformation.CreateStackStepConfiguration")

public class CloudformationCreateStackStepConfiguration {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> stackName;
  @NotNull @JsonProperty("templateFile") CloudformationTemplateFilesWrapper templateFilesWrapper;
  @JsonProperty("parameters") ParameterField<List<CloudformationParametersFilesWrapper>> parametersFilesWrapper;
  @NotNull @JsonProperty("awsCloudProvider") ParameterField<CloudformatiomAwsCloudProvider> awsCloudProvider;
  @NotNull @JsonProperty("awsRegion") ParameterField<CloudformationAwsRegions> awsRegion;
  @JsonProperty("awsRoleARN") ParameterField<CloudformationAwsRoleARN> awsRoleARN;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @JsonProperty("awsCapabilities")
  ParameterField<List<String>> awsCapabilities;
  List<NGVariable> tags;
  @JsonProperty("cloudformationSkipWaitForResources") ParameterField<Boolean> cloudformationSkipWaitForResources;
  @JsonProperty("cloudformationSkipWaitForResourcesTimeout")
  ParameterField<Integer> cloudformationSkipWaitForResourcesTimeout;
  @JsonProperty("skipBasedOnStackStatuses") ParameterField<List<String>> skipBasedOnStackStatuses;

  void validateParams() {
    Validator.notNullCheck("AWS cloud provider is null", awsCloudProvider);
    Validator.notNullCheck("AWS region  is null", awsRegion);
  }
}
