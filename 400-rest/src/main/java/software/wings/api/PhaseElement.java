/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.api;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.context.ContextElementType;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.NameValuePair;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.mongodb.morphia.annotations.Transient;

/**
 * The Class PhaseElement.
 *
 * @author Rishi
 */

@Data
@Builder
@OwnedBy(CDC)
@TargetModule(_957_CG_BEANS)
public class PhaseElement implements ContextElement {
  public static final String PHASE_PARAM = "PHASE_PARAM";

  @Inject @Transient private transient ArtifactService artifactService;
  @Inject @Transient private transient FeatureFlagService featureFlagService;

  private String uuid;
  private String phaseName;
  private ServiceElement serviceElement;
  private String appId;
  @Getter(AccessLevel.NONE) private String infraMappingId;
  private String deploymentType;
  private String phaseNameForRollback;
  @Builder.Default private List<NameValuePair> variableOverrides = new ArrayList<>();
  private String rollbackArtifactId;
  private String infraDefinitionId;
  private String workflowExecutionId;
  private boolean rollback;
  private boolean onDemandRollback;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.PARAM;
  }

  @Override
  public String getName() {
    return PHASE_PARAM;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }

  public String getPhaseExecutionIdForSweepingOutput() {
    return workflowExecutionId + uuid + phaseName;
  }
}
