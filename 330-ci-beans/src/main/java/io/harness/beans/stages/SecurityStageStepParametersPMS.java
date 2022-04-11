/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.stages;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.build.BuildStatusUpdateParameter;
import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure.Type;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.ParallelStepElementConfig;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.plan.creation.PlanCreatorUtils;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("securityStageStepParameters")
@OwnedBy(HarnessTeam.STO)
@RecasterAlias("io.harness.beans.stages.SecurityStageStepParametersPMS")
public class SecurityStageStepParametersPMS implements SpecParameters, StepParameters {
  Infrastructure infrastructure;
  List<DependencyElement> dependencies;
  ParameterField<List<String>> sharedPaths;
  ParameterField<Boolean> enableCloneRepo;
  BuildStatusUpdateParameter buildStatusUpdateParameter;
  List<String> stepIdentifiers;
  String childNodeID;

  public static SecurityStageStepParametersPMS getStepParameters(StageElementConfig stageElementConfig,
      String childNodeID, BuildStatusUpdateParameter buildStatusUpdateParameter, PlanCreationContext ctx) {
    if (stageElementConfig == null) {
      return SecurityStageStepParametersPMS.builder().childNodeID(childNodeID).build();
    }
    SecurityStageConfig securityStageConfig = (SecurityStageConfig) stageElementConfig.getStageType();

    Infrastructure infrastructure = getInfrastructure(stageElementConfig, ctx);

    List<String> stepIdentifiers = getStepIdentifiers(securityStageConfig);

    return SecurityStageStepParametersPMS.builder()
        .buildStatusUpdateParameter(buildStatusUpdateParameter)
        .infrastructure(infrastructure)
        .dependencies(securityStageConfig.getServiceDependencies().getValue())
        .childNodeID(childNodeID)
        .sharedPaths(securityStageConfig.getSharedPaths())
        .enableCloneRepo(securityStageConfig.getCloneCodebase())
        .stepIdentifiers(stepIdentifiers)
        .build();
  }

  public static Infrastructure getInfrastructure(StageElementConfig stageElementConfig, PlanCreationContext ctx) {
    SecurityStageConfig securityStageConfig = (SecurityStageConfig) stageElementConfig.getStageType();

    Infrastructure infrastructure = securityStageConfig.getInfrastructure();
    if (infrastructure == null) {
      throw new CIStageExecutionException("Infrastructure is mandatory for execution");
    }
    if (securityStageConfig.getInfrastructure().getType() == Type.USE_FROM_STAGE) {
      UseFromStageInfraYaml useFromStageInfraYaml = (UseFromStageInfraYaml) securityStageConfig.getInfrastructure();
      if (useFromStageInfraYaml.getUseFromStage() != null) {
        YamlField yamlField = ctx.getCurrentField();
        String identifier = useFromStageInfraYaml.getUseFromStage();
        SecurityStageConfig stageConfig = getStageConfig(yamlField, identifier);
        infrastructure = stageConfig.getInfrastructure();
      }
    }

    return infrastructure;
  }

  private static SecurityStageConfig getStageConfig(YamlField yamlField, String identifier) {
    try {
      YamlField stageYamlField = PlanCreatorUtils.getStageConfig(yamlField, identifier);
      StageElementConfig stageElementConfig =
          YamlUtils.read(YamlUtils.writeYamlString(stageYamlField), StageElementConfig.class);
      return (SecurityStageConfig) stageElementConfig.getStageType();

    } catch (Exception ex) {
      throw new CIStageExecutionException(
          "Failed to deserialize SecurityStage for use from stage identifier: " + identifier, ex);
    }
  }

  private static List<String> getStepIdentifiers(SecurityStageConfig securityStageConfig) {
    List<String> stepIdentifiers = new ArrayList<>();
    securityStageConfig.getExecution().getSteps().forEach(
        executionWrapper -> addStepIdentifier(executionWrapper, stepIdentifiers));
    return stepIdentifiers;
  }

  private static void addStepIdentifier(ExecutionWrapperConfig executionWrapper, List<String> stepIdentifiers) {
    if (executionWrapper != null) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = getStepElementConfig(executionWrapper);
        stepIdentifiers.add(stepElementConfig.getIdentifier());
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig = getParallelStepElementConfig(executionWrapper);
        parallelStepElementConfig.getSections().forEach(section -> addStepIdentifier(section, stepIdentifiers));
      } else {
        throw new InvalidRequestException("Only Parallel or StepElement is supported");
      }
    }
  }

  private static StepElementConfig getStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getStep().toString(), StepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig step node", ex);
    }
  }

  private static ParallelStepElementConfig getParallelStepElementConfig(ExecutionWrapperConfig executionWrapperConfig) {
    try {
      return YamlUtils.read(executionWrapperConfig.getParallel().toString(), ParallelStepElementConfig.class);
    } catch (Exception ex) {
      throw new CIStageExecutionException("Failed to deserialize ExecutionWrapperConfig parallel node", ex);
    }
  }
}
