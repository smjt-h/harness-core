/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.applicationmanifest.HelmChartService;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * The Class that extends the functionality of WorflowStandardParams.
 */
@OwnedBy(CDC)
public class WorkflowStandardParamsExtensionService {
  @Inject private transient AppService appService;
  @Inject private transient AccountService accountService;
  @Inject private transient ArtifactService artifactService;
  @Inject private transient EnvironmentService environmentService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient MainConfiguration configuration;
  @Inject private transient ArtifactStreamService artifactStreamService;
  @Inject private transient ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private transient HelmChartService helmChartService;

  public HelmChart getHelmChartForService(WorkflowStandardParams workflowStandardParams, String serviceId) {
    List<HelmChart> helmCharts = getHelmCharts(workflowStandardParams);
    if (isEmpty(helmCharts)) {
      return null;
    }

    return helmCharts.stream().filter(helmChart -> serviceId.equals(helmChart.getServiceId())).findFirst().orElse(null);
  }

  public List<HelmChart> getHelmCharts(WorkflowStandardParams workflowStandardParams) {
    if (isEmpty(workflowStandardParams.getHelmChartIds())) {
      return null;
    }

    return helmChartService.listByIds(
        getApp(workflowStandardParams).getAccountId(), workflowStandardParams.getHelmChartIds());
  }

  /**
   * Gets app.
   *
   * @return the app
   */
  public Application getApp(WorkflowStandardParams workflowStandardParams) {
    if (workflowStandardParams.getAppId() == null) {
      return null;
    }
    return appService.getApplicationWithDefaults(workflowStandardParams.getAppId());
  }

  public Application fetchRequiredApp(WorkflowStandardParams workflowStandardParams) {
    Application application = getApp(workflowStandardParams);
    if (application == null) {
      throw new InvalidRequestException("App cannot be null");
    }
    return application;
  }

  public Account getAccount(WorkflowStandardParams workflowStandardParams) {
    String accountId = getApp(workflowStandardParams) == null ? null : getApp(workflowStandardParams).getAccountId();
    if (accountId == null) {
      return null;
    }

    return accountService.getAccountWithDefaults(accountId);
  }

  /**
   * Gets env.
   *
   * @return the env
   */
  public Environment getEnv(WorkflowStandardParams workflowStandardParams) {
    if (workflowStandardParams.getEnvId() == null) {
      return null;
    }

    return environmentService.get(workflowStandardParams.getAppId(), workflowStandardParams.getEnvId(), false);
  }

  public Environment fetchRequiredEnv(WorkflowStandardParams workflowStandardParams) {
    Environment environment = getEnv(workflowStandardParams);
    if (environment == null) {
      throw new InvalidRequestException("Env cannot be null");
    }
    return environment;
  }

  /**
   * Gets artifacts.
   *
   * @return the artifacts
   */
  public List<Artifact> getArtifacts(WorkflowStandardParams workflowStandardParams) {
    if (isEmpty(workflowStandardParams.getArtifactIds())) {
      return null;
    }
    List<Artifact> list = new ArrayList<>();
    for (String artifactId : workflowStandardParams.getArtifactIds()) {
      Artifact artifact = artifactService.get(artifactId);
      if (artifact != null) {
        list.add(artifact);
      }
    }

    return list;
  }

  /**
   * Gets rollback artifacts.
   *
   * @return the rollback artifacts
   */
  public List<Artifact> getRollbackArtifacts(WorkflowStandardParams workflowStandardParams) {
    if (isEmpty(workflowStandardParams.getRollbackArtifactIds())) {
      return null;
    }

    List<Artifact> list = new ArrayList<>();
    for (String rollbackArtifactId : workflowStandardParams.getRollbackArtifactIds()) {
      Artifact rollbackArtifact = artifactService.get(rollbackArtifactId);
      if (rollbackArtifact != null) {
        list.add(rollbackArtifact);
      }
    }

    return list;
  }

  /**
   * Gets artifact for service.
   *
   * @param serviceId the service id
   * @return the artifact for service
   */
  public Artifact getArtifactForService(WorkflowStandardParams workflowStandardParams, String serviceId) {
    List<Artifact> artifacts = getArtifacts(workflowStandardParams);
    if (isEmpty(artifacts)) {
      return null;
    }

    List<String> artifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
    if (isEmpty(artifactStreamIds)) {
      return null;
    }

    return artifacts.stream()
        .filter(artifact -> artifactStreamIds.contains(artifact.getArtifactStreamId()))
        .findFirst()
        .orElse(null);
  }

  /**
   * Gets rollback artifact for service.
   *
   * @param serviceId the service id
   * @return the rollback artifact for service
   */
  public Artifact getRollbackArtifactForService(WorkflowStandardParams workflowStandardParams, String serviceId) {
    List<Artifact> rollbackArtifacts = getRollbackArtifacts(workflowStandardParams);
    if (isEmpty(rollbackArtifacts)) {
      return null;
    }

    List<String> rollbackArtifactStreamIds = artifactStreamServiceBindingService.listArtifactStreamIds(serviceId);
    if (isEmpty(rollbackArtifactStreamIds)) {
      return null;
    }

    return rollbackArtifacts.stream()
        .filter(rollbackArtifact -> rollbackArtifactStreamIds.contains(rollbackArtifact.getArtifactStreamId()))
        .findFirst()
        .orElse(null);
  }
}
