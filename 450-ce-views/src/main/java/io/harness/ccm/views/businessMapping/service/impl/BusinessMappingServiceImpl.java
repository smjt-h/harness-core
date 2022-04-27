/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.businessMapping.service.impl;

import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_ACCOUNT_FIELD;

import io.harness.ccm.commons.service.intf.EntityMetadataService;
import io.harness.ccm.views.businessMapping.dao.BusinessMappingDao;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.businessMapping.service.intf.BusinessMappingService;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.utils.AwsAccountFieldUtils;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class BusinessMappingServiceImpl implements BusinessMappingService {
  @Inject private BusinessMappingDao businessMappingDao;
  @Inject private EntityMetadataService entityMetadataService;

  @Override
  public BusinessMapping save(BusinessMapping businessMapping) {
    validateBusinessMapping(businessMapping);
    return businessMappingDao.save(businessMapping);
  }

  @Override
  public BusinessMapping get(String uuid, String accountId) {
    final BusinessMapping businessMapping = businessMappingDao.get(uuid, accountId);
    modifyBusinessMapping(businessMapping);
    return businessMapping;
  }

  @Override
  public BusinessMapping get(String uuid) {
    final BusinessMapping businessMapping = businessMappingDao.get(uuid);
    modifyBusinessMapping(businessMapping);
    return businessMapping;
  }

  @Override
  public BusinessMapping update(BusinessMapping businessMapping) {
    return businessMappingDao.update(businessMapping);
  }

  @Override
  public boolean delete(String uuid, String accountIdentifier) {
    return businessMappingDao.delete(uuid, accountIdentifier);
  }

  @Override
  public List<BusinessMapping> list(String accountId) {
    List<BusinessMapping> businessMappings = businessMappingDao.findByAccountId(accountId);
    businessMappings.forEach(this::modifyBusinessMapping);
    businessMappings.sort(Comparator.comparing(BusinessMapping::getLastUpdatedAt).reversed());
    return businessMappings;
  }

  @Override
  public List<ViewField> getBusinessMappingViewFields(String accountId) {
    List<BusinessMapping> businessMappingList = businessMappingDao.findByAccountId(accountId);
    List<ViewField> viewFieldList = new ArrayList<>();
    for (BusinessMapping businessMapping : businessMappingList) {
      viewFieldList.add(ViewField.builder()
                            .fieldId(businessMapping.getUuid())
                            .fieldName(businessMapping.getName())
                            .identifier(ViewFieldIdentifier.BUSINESS_MAPPING)
                            .identifierName(ViewFieldIdentifier.BUSINESS_MAPPING.getDisplayName())
                            .build());
    }
    return viewFieldList;
  }

  private void validateBusinessMapping(final BusinessMapping businessMapping) {
    // TODO: Validate if Business Mapping already exists or not
    updateBusinessMapping(businessMapping);
  }

  private void updateBusinessMapping(final BusinessMapping businessMapping) {
    if (Objects.nonNull(businessMapping.getCostTargets())) {
      businessMapping.getCostTargets().forEach(
          costTarget -> removeAwsAccountNameFromAccountValues(costTarget.getRules()));
    }
    if (Objects.nonNull(businessMapping.getSharedCosts())) {
      businessMapping.getSharedCosts().forEach(
          sharedCost -> removeAwsAccountNameFromAccountValues(sharedCost.getRules()));
    }
  }

  private void removeAwsAccountNameFromAccountValues(final List<ViewRule> rules) {
    if (Objects.nonNull(rules)) {
      rules.forEach(viewRule -> {
        if (Objects.nonNull(viewRule.getViewConditions())) {
          viewRule.getViewConditions().forEach(viewCondition -> {
            final ViewIdCondition viewIdCondition = (ViewIdCondition) viewCondition;
            if (AWS_ACCOUNT_FIELD.equals(viewIdCondition.getViewField().getFieldName())) {
              viewIdCondition.setValues(removeAccountNameFromValues(viewIdCondition.getValues()));
            }
          });
        }
      });
    }
  }

  private List<String> removeAccountNameFromValues(final List<String> values) {
    return values.stream().map(AwsAccountFieldUtils::removeAwsAccountNameFromValue).collect(Collectors.toList());
  }

  private void modifyBusinessMapping(final BusinessMapping businessMapping) {
    if (Objects.nonNull(businessMapping.getCostTargets())) {
      businessMapping.getCostTargets().forEach(
          costTarget -> mergeAwsAccountNameWithAccountValues(costTarget.getRules(), businessMapping.getAccountId()));
    }
    if (Objects.nonNull(businessMapping.getSharedCosts())) {
      businessMapping.getSharedCosts().forEach(
          sharedCost -> mergeAwsAccountNameWithAccountValues(sharedCost.getRules(), businessMapping.getAccountId()));
    }
  }

  private void mergeAwsAccountNameWithAccountValues(final List<ViewRule> rules, final String accountId) {
    if (Objects.nonNull(rules)) {
      rules.forEach(viewRule -> {
        if (Objects.nonNull(viewRule.getViewConditions())) {
          viewRule.getViewConditions().forEach(viewCondition -> {
            final ViewIdCondition viewIdCondition = (ViewIdCondition) viewCondition;
            if (AWS_ACCOUNT_FIELD.equals(viewIdCondition.getViewField().getFieldName())) {
              viewIdCondition.setValues(mergeAccountNameWithValues(viewIdCondition.getValues(), accountId));
            }
          });
        }
      });
    }
  }

  private List<String> mergeAccountNameWithValues(final List<String> values, final String accountId) {
    final Map<String, String> entityIdToName =
        entityMetadataService.getEntityIdToNameMapping(values, accountId, AWS_ACCOUNT_FIELD);
    return values.stream()
        .map(value -> AwsAccountFieldUtils.mergeAwsAccountIdAndName(value, entityIdToName.get(value)))
        .collect(Collectors.toList());
  }
}
