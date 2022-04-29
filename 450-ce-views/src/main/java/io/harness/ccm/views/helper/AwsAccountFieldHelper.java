/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.helper;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.constants.ViewFieldConstants.AWS_ACCOUNT_FIELD;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.service.intf.EntityMetadataService;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(CE)
public class AwsAccountFieldHelper {
  @Inject private EntityMetadataService entityMetadataService;

  private static final Pattern ACCOUNT_ID_EXTRACT_PATTERN = Pattern.compile("\\((.*?)\\)");

  public static String mergeAwsAccountIdAndName(final String accountId, final String accountName) {
    String accountDetails = accountId;
    if (!Strings.isNullOrEmpty(accountName)) {
      accountDetails = accountName + " (" + accountId + ")";
    }
    return accountDetails;
  }

  public static String removeAwsAccountNameFromValue(final String value) {
    String accountId = value;
    final Matcher matcher = ACCOUNT_ID_EXTRACT_PATTERN.matcher(value);
    if (matcher.find()) {
      accountId = matcher.group(1);
    }
    return accountId;
  }

  public void removeAwsAccountNameFromAccountRules(final List<ViewRule> rules) {
    if (Objects.nonNull(rules)) {
      rules.forEach(viewRule -> {
        if (Objects.nonNull(viewRule.getViewConditions())) {
          viewRule.getViewConditions().forEach(viewCondition -> {
            final ViewIdCondition viewIdCondition = (ViewIdCondition) viewCondition;
            if (Objects.nonNull(viewIdCondition.getViewField())
                && AWS_ACCOUNT_FIELD.equals(viewIdCondition.getViewField().getFieldName())) {
              viewIdCondition.setValues(removeAwsAccountNameFromValues(viewIdCondition.getValues()));
            }
          });
        }
      });
    }
  }

  public static List<String> removeAwsAccountNameFromValues(final List<String> values) {
    return values.stream().map(AwsAccountFieldHelper::removeAwsAccountNameFromValue).collect(Collectors.toList());
  }

  public void mergeAwsAccountNameInAccountRules(final List<ViewRule> rules, final String accountId) {
    if (Objects.nonNull(rules)) {
      rules.forEach(viewRule -> {
        if (Objects.nonNull(viewRule.getViewConditions())) {
          viewRule.getViewConditions().forEach(viewCondition -> {
            final ViewIdCondition viewIdCondition = (ViewIdCondition) viewCondition;
            if (Objects.nonNull(viewIdCondition.getViewField())
                && AWS_ACCOUNT_FIELD.equals(viewIdCondition.getViewField().getFieldName())) {
              viewIdCondition.setValues(mergeAwsAccountNameWithValues(viewIdCondition.getValues(), accountId));
            }
          });
        }
      });
    }
  }

  public List<String> mergeAwsAccountNameWithValues(final List<String> values, final String accountId) {
    final Map<String, String> entityIdToName =
        entityMetadataService.getEntityIdToNameMapping(values, accountId, AWS_ACCOUNT_FIELD);
    return values.stream()
        .map(value -> mergeAwsAccountIdAndName(value, entityIdToName.get(value)))
        .collect(Collectors.toList());
  }
}
