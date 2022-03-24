/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ServerlessAwsLambdaManifestOutcome;
import io.harness.delegate.beans.instancesync.ServerInstanceInfo;
import io.harness.delegate.beans.instancesync.mapper.ServerlessAwsLambdaFunctionToServerInstanceInfoMapper;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaDeployResult;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaRollbackResult;
import io.harness.delegate.beans.serverless.ServerlessDeployResult;
import io.harness.delegate.beans.serverless.ServerlessRollbackResult;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.response.ServerlessDeployResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class ServerlessAwsLambdaStepHelper implements ServerlessStepHelper {
  @Inject private ServerlessStepCommonHelper serverlessStepCommonHelper;

  @Override
  public ManifestOutcome getServerlessManifestOutcome(@NotEmpty Collection<ManifestOutcome> manifestOutcomes) {
    List<ManifestOutcome> serverlessManifests =
        manifestOutcomes.stream()
            .filter(manifestOutcome -> ManifestType.ServerlessAwsLambda.equals(manifestOutcome.getType()))
            .collect(Collectors.toList());
    if (isEmpty(serverlessManifests)) {
      throw new InvalidRequestException("Manifests are mandatory for Serverless Aws Lambda step", USER);
    }
    if (serverlessManifests.size() > 1) {
      throw new InvalidRequestException("There can be only a single manifest for Serverless Aws Lambda step", USER);
    }
    return serverlessManifests.get(0);
  }

  @Override
  public String getConfigOverridePath(ManifestOutcome manifestOutcome) {
    if (manifestOutcome instanceof ServerlessAwsLambdaManifestOutcome) {
      ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
          (ServerlessAwsLambdaManifestOutcome) manifestOutcome;
      return getParameterFieldValue(serverlessAwsLambdaManifestOutcome.getConfigOverridePath());
    }
    throw new UnsupportedOperationException(
        format("Unsupported serverless manifest type: [%s]", manifestOutcome.getType()));
  }

  @Override
  public ServerlessDeployConfig getServerlessDeployConfig(ServerlessSpecParameters serverlessSpecParameters) {
    if (serverlessSpecParameters instanceof ServerlessAwsLambdaDeployStepParameters) {
      ServerlessAwsLambdaDeployStepParameters serverlessAwsLambdaDeployStepParameters =
          (ServerlessAwsLambdaDeployStepParameters) serverlessSpecParameters;
      return ServerlessAwsLambdaDeployConfig.builder()
          .commandOptions(serverlessAwsLambdaDeployStepParameters.getCommandOptions().getValue())
          .build();
    }
    throw new UnsupportedOperationException(
        format("Unsupported serverless spec : [%s]", serverlessSpecParameters.getClass()));
  }

  @Override
  public ServerlessManifestConfig getServerlessManifestConfig(
      ManifestOutcome manifestOutcome, Ambiance ambiance, Map<String, Object> manifestParams) {
    if (manifestOutcome instanceof ServerlessAwsLambdaManifestOutcome) {
      ServerlessAwsLambdaManifestOutcome serverlessAwsLambdaManifestOutcome =
          (ServerlessAwsLambdaManifestOutcome) manifestOutcome;
      Pair<String, String> manifestFilePathContent =
          (Pair<String, String>) manifestParams.get("manifestFilePathContent");
      String manifestFileOverrideContent = (String) manifestParams.get("manifestFileOverrideContent");
      GitStoreConfig gitStoreConfig = (GitStoreConfig) serverlessAwsLambdaManifestOutcome.getStore();
      return ServerlessAwsLambdaManifestConfig.builder()
          .manifestPath(manifestFilePathContent.getKey())
          .manifestContent(manifestFileOverrideContent)
          .gitStoreDelegateConfig(
              serverlessStepCommonHelper.getGitStoreDelegateConfig(ambiance, gitStoreConfig, manifestOutcome))
          .build();
    }
    throw new UnsupportedOperationException(
        format("Unsupported serverless manifest type: [%s]", manifestOutcome.getType()));
  }

  @Override
  public List<ServerInstanceInfo> getServerlessDeployFunctionInstanceInfo(
      ServerlessDeployResult serverlessDeployResult) {
    if (serverlessDeployResult instanceof ServerlessAwsLambdaDeployResult) {
      ServerlessAwsLambdaDeployResult serverlessAwsLambdaDeployResult =
          (ServerlessAwsLambdaDeployResult) serverlessDeployResult;
      return ServerlessAwsLambdaFunctionToServerInstanceInfoMapper.toServerInstanceInfoList(
          serverlessAwsLambdaDeployResult.getFunctions(), serverlessAwsLambdaDeployResult.getRegion(),
          serverlessAwsLambdaDeployResult.getStage(), serverlessAwsLambdaDeployResult.getService());
    }
    throw new UnsupportedOperationException(
        format("Unsupported serverless deploy instance: [%s]", serverlessDeployResult.getClass()));
  }

  @Override
  public List<ServerInstanceInfo> getServerlessRollbackFunctionInstanceInfo(
      ServerlessRollbackResult serverlessRollbackResult) {
    if (serverlessRollbackResult instanceof ServerlessAwsLambdaRollbackResult) {
      ServerlessAwsLambdaRollbackResult serverlessAwsLambdaRollbackResult =
          (ServerlessAwsLambdaRollbackResult) serverlessRollbackResult;
      return ServerlessAwsLambdaFunctionToServerInstanceInfoMapper.toServerInstanceInfoList(
          serverlessAwsLambdaRollbackResult.getFunctions(), serverlessAwsLambdaRollbackResult.getRegion(),
          serverlessAwsLambdaRollbackResult.getStage(), serverlessAwsLambdaRollbackResult.getService());
    }
    throw new UnsupportedOperationException(
        format("Unsupported serverless deploy instance: [%s]", serverlessRollbackResult.getClass()));
  }

  public String getPreviousVersion(ServerlessDeployResponse serverlessDeployResponse) {
    ServerlessAwsLambdaDeployResult serverlessAwsLambdaDeployResult =
        (ServerlessAwsLambdaDeployResult) serverlessDeployResponse.getServerlessDeployResult();
    return serverlessAwsLambdaDeployResult.getPreviousVersionTimeStamp();
  }
}
