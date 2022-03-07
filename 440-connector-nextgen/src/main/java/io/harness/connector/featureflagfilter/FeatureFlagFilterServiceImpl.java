package io.harness.connector.featureflagfilter;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDP)
public class FeatureFlagFilterServiceImpl implements FeatureFlagFilterService {
  @Inject private FeatureFlagHelper featureFlagHelper;

  public <T extends Enum<?>> List<T> filterEnum(
      String accountId, FeatureName featureName, Class<T> enumClass, FeatureFlagFilterContext context) {
    return Arrays.stream(enumClass.getEnumConstants())
        .filter(enumType -> {
          Set<Enum<?>> enums = context.getEnumTypeFeatureFlagMap().get(featureName);
          if (!isEmpty(enums) && enums.contains(enumType)) {
            return featureFlagHelper.isEnabled(accountId, featureName);
          }
          return true;
        })
        .collect(Collectors.toList());
  }
}
