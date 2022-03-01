/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.EngineExpressionSecretUtils;
import io.harness.pms.expression.EngineExpressionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.CDP)
public class CloudformationConfigDAL {
  @Inject private HPersistence persistence;
  @Inject private EngineExpressionService engineExpressionService;

  void saveCloudformationStack(@NonNull CloudformationConfig config) {
    CloudformationConfig secretsRevertedConfig =
        (CloudformationConfig) EngineExpressionSecretUtils.revertSecrets(config);
    persistence.save(secretsRevertedConfig);
  }
  public void deleteCloudformationStack(@NonNull Ambiance ambiance, @NonNull String entityId) {
    // TODO: Remove comment when the DB is implemented
    //         persistence.delete(persistence.createQuery(CloudformationConfig.class)
    //                 .filter(CloudformationConfig.CloudformationConfigKeys.accountId,
    //                 AmbianceUtils.getAccountId(ambiance))
    //                 .filter(CloudformationConfig.CloudformationConfigKeys.entityId, entityId)
    //                 .filter(CloudformationConfig.CloudformationConfigKeys.orgId,
    //                 AmbianceUtils.getOrgIdentifier(ambiance))
    //                 .filter(CloudformationConfig.CloudformationConfigKeys.projectId,
    //                 AmbianceUtils.getProjectIdentifier(ambiance)));
  }
}
