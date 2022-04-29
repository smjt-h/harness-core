/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.serverless;

import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.k8s.manifest.ManifestHelper.normalizeFolderPath;

import static java.lang.String.format;

import io.harness.cdng.CDStepHelper;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.exception.GeneralException;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ServerlessStepUtils extends CDStepHelper {
  @Inject private ServerlessEntityHelper serverlessEntityHelper;
  private static final String SERVERLESS_YAML_REGEX = ".*serverless\\.yaml";
  private static final String SERVERLESS_YML_REGEX = ".*serverless\\.yml";
  private static final String SERVERLESS_JSON_REGEX = ".*serverless\\.json";

  public GitStoreDelegateConfig getGitStoreDelegateConfig(
      Ambiance ambiance, GitStoreConfig gitStoreConfig, ManifestOutcome manifestOutcome) {
    String connectorId = gitStoreConfig.getConnectorRef().getValue();
    String validationMessage = format("Serverless manifest with Id [%s]", manifestOutcome.getIdentifier());
    ConnectorInfoDTO connectorDTO = getConnectorDTO(connectorId, ambiance);
    validateManifest(gitStoreConfig.getKind(), connectorDTO, validationMessage);
    List<String> gitPaths = getFolderPathsForManifest(gitStoreConfig);
    return getGitStoreDelegateConfig(gitStoreConfig, connectorDTO, manifestOutcome, gitPaths, ambiance);
  }

  private ConnectorInfoDTO getConnectorDTO(String connectorId, Ambiance ambiance) {
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    return serverlessEntityHelper.getConnectorInfoDTO(connectorId, ngAccess);
  }

  private List<String> getFolderPathsForManifest(GitStoreConfig gitStoreConfig) {
    List<String> folderPaths = new ArrayList<>();

    List<String> paths = getParameterFieldValue(gitStoreConfig.getPaths());
    if ((paths != null) && (!paths.isEmpty())) {
      folderPaths.add(normalizeFolderPath(paths.get(0)));
    } else {
      folderPaths.add(normalizeFolderPath(getParameterFieldValue(gitStoreConfig.getFolderPath())));
    }
    return folderPaths;
    // todo: add error handling
  }

  public String getManifestDefaultFileName(String manifestFilePath) {
    if (Pattern.matches(SERVERLESS_YAML_REGEX, manifestFilePath)) {
      return "serverless.yaml";
    } else if (Pattern.matches(SERVERLESS_YML_REGEX, manifestFilePath)) {
      return "serverless.yml";
    } else if (Pattern.matches(SERVERLESS_JSON_REGEX, manifestFilePath)) {
      return "serverless.json";
    } else {
      throw new GeneralException("Invalid serverless file name");
    }
  }
}
