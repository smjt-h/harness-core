/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.steps.stepinfo;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.beans.SwaggerConstants.INTEGER_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.beans.SwaggerConstants.STRING_MAP_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.yaml.extended.CIShellType;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.beans.yaml.extended.TIBuildTool;
import io.harness.beans.yaml.extended.TILanguage;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.core.VariableExpression;
import io.harness.yaml.core.variables.OutputNGVariable;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("RunTests")
@TypeAlias("runTestsStepInfo")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.stepinfo.RunTestsStepInfo")
public class RunTestsStepInfo implements CIStepInfo {
  @VariableExpression(skipVariableExpression = true) public static final int DEFAULT_RETRY = 0;
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  private String uuid;
  // Keeping the timeout to a day as its a test step and might take lot of time
  @VariableExpression(skipVariableExpression = true) public static final int DEFAULT_TIMEOUT = 60 * 60 * 24; // 24 hour;

  @JsonIgnore public static final TypeInfo typeInfo = TypeInfo.builder().stepInfoType(CIStepInfoType.RUN_TESTS).build();

  @JsonIgnore
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(CIStepInfoType.RUN_TESTS.getDisplayName())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String identifier;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) private String name;
  @VariableExpression(skipVariableExpression = true) @Min(MIN_RETRY) @Max(MAX_RETRY) private int retry;

  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> args;
  @NotNull
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = "io.harness.beans.yaml.extended.TILanguage")
  private ParameterField<TILanguage> language;
  @NotNull
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = "io.harness.beans.yaml.extended.TIBuildTool")
  private ParameterField<TIBuildTool> buildTool;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> packages;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> testAnnotations;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = "io.harness.beans.yaml.extended.reports.UnitTestReport")
  private ParameterField<UnitTestReport> reports;
  @YamlSchemaTypes({string})
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  private ParameterField<Boolean> runOnlySelectedTests;

  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> image;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> connectorRef;
  private ContainerResource resources;
  @YamlSchemaTypes(value = {runtime})
  @VariableExpression(skipVariableExpression = true)
  @ApiModelProperty(dataType = "[Lio.harness.yaml.core.variables.OutputNGVariable;")
  private ParameterField<List<OutputNGVariable>> outputVariables;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = STRING_MAP_CLASSPATH)
  private ParameterField<Map<String, String>> envVariables;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> preCommand;
  @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> postCommand;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  private ParameterField<Boolean> privileged;
  @YamlSchemaTypes({string}) @ApiModelProperty(dataType = INTEGER_CLASSPATH) private ParameterField<Integer> runAsUser;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = "io.harness.beans.yaml.extended.ImagePullPolicy")
  private ParameterField<ImagePullPolicy> imagePullPolicy;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = "io.harness.beans.yaml.extended.CIShellType")
  private ParameterField<CIShellType> shell;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "args", "language", "buildTool", "image", "connectorRef",
      "resources", "reports", "testAnnotations", "packages", "runOnlySelectedTests", "preCommand", "postCommand",
      "outputVariables", "envVariables", "privileged", "runAsUser", "imagePullPolicy", "shell"})
  public RunTestsStepInfo(String identifier, String name, Integer retry, ParameterField<String> args,
      ParameterField<TILanguage> language, ParameterField<TIBuildTool> buildTool, ParameterField<String> image,
      ParameterField<String> connectorRef, ContainerResource resources, ParameterField<UnitTestReport> reports,
      ParameterField<String> testAnnotations, ParameterField<String> packages,
      ParameterField<Boolean> runOnlySelectedTests, ParameterField<String> preCommand,
      ParameterField<String> postCommand, ParameterField<List<OutputNGVariable>> outputVariables,
      ParameterField<Map<String, String>> envVariables, ParameterField<Boolean> privileged,
      ParameterField<Integer> runAsUser, ParameterField<ImagePullPolicy> imagePullPolicy,
      ParameterField<CIShellType> shell) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.args = args;
    this.language = language;
    this.buildTool = buildTool;
    this.image = image;
    this.connectorRef = connectorRef;
    this.resources = resources;
    this.reports = reports;
    this.testAnnotations = testAnnotations;
    this.packages = packages;
    this.runOnlySelectedTests = runOnlySelectedTests;
    this.preCommand = preCommand;
    this.postCommand = postCommand;
    this.outputVariables = outputVariables;
    this.envVariables = envVariables;
    this.privileged = privileged;
    this.runAsUser = runAsUser;
    this.imagePullPolicy = imagePullPolicy;
    this.shell = shell;
  }

  @Override
  public long getDefaultTimeout() {
    return DEFAULT_TIMEOUT;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Override
  public StepType getStepType() {
    return STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }
}
