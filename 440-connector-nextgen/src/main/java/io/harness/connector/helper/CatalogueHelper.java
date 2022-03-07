/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.helper;

import io.harness.beans.FeatureName;
import io.harness.connector.ConnectorCatalogueItem;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorRegistryFactory;
import io.harness.connector.featureflagfilter.CdFeatureFlagFilterContext;
import io.harness.connector.featureflagfilter.FeatureFlagFilterService;
import io.harness.delegate.beans.connector.ConnectorType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class CatalogueHelper {
  @Inject CdFeatureFlagFilterContext cdFeatureFlagFilterContext;
  @Inject FeatureFlagFilterService featureFlagFilterService;

  public List<ConnectorCatalogueItem> getConnectorTypeToCategoryMapping(String accountIdentifier) {
    final Map<ConnectorCategory, List<ConnectorType>> connectorCategoryListMap =
        featureFlagFilterService
            .filterEnum(accountIdentifier, FeatureName.SSH_NG, ConnectorType.class, cdFeatureFlagFilterContext)
            .stream()
            .collect(Collectors.groupingBy(ConnectorRegistryFactory::getConnectorCategory));
    return connectorCategoryListMap.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry
            -> ConnectorCatalogueItem.builder()
                   .category(entry.getKey())
                   .connectors(new HashSet<>(entry.getValue()))
                   .build())
        .collect(Collectors.toList());
  }
}
