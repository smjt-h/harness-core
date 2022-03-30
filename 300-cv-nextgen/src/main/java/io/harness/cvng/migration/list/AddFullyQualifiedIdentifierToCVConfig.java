/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.migration.list;

import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.cvng.migration.CVNGMigration;
import io.harness.cvng.migration.beans.ChecklistItem;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateResults;

@Slf4j
public class AddFullyQualifiedIdentifierToCVConfig implements CVNGMigration {
  @Inject private HPersistence hPersistence;

  @Override
  public void migrate() {
    log.info("Begin migration for updating CVConfig fullyQualifiedIdentifier");
    Query<CVConfig> cvConfigQuery = hPersistence.createQuery(CVConfig.class);

    try (HIterator<CVConfig> iterator = new HIterator<>(cvConfigQuery.fetch())) {
      while (iterator.hasNext()) {
        CVConfig cvConfig = iterator.next();
        UpdateResults updateResults = hPersistence.update(cvConfig,
            hPersistence.createUpdateOperations(CVConfig.class)
                .set(CVConfigKeys.fullyQualifiedIdentifier, cvConfig.getIdentifier()));
        log.info("Updated CVConfig {}", updateResults);
      }
    }
  }

  @Override
  public ChecklistItem whatHappensOnRollback() {
    return ChecklistItem.NA;
  }

  @Override
  public ChecklistItem whatHappensIfOldVersionIteratorPicksMigratedEntity() {
    return ChecklistItem.NA;
  }
}
