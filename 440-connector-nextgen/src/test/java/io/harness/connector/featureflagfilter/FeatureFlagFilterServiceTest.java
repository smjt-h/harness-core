package io.harness.connector.featureflagfilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FeatureFlagFilterServiceTest extends CategoryTest {
  @Mock FeatureFlagHelper featureFlagHelper;
  @InjectMocks FeatureFlagFilterServiceImpl featureFlagFilterService;

  @Test
  @Owner(developers = OwnerRule.BOJAN)
  @Category(UnitTests.class)
  public void testFilterEnumFFEnabled() {
    doReturn(true).when(featureFlagHelper).isEnabled(any(), any());
    List<WeekdaysEnum> types = featureFlagFilterService.filterEnum(
        "accountId", FeatureName.SSH_NG, WeekdaysEnum.class, new TestFeatureFlagFilterContext());

    assertThat(types.size()).isEqualTo(7);
  }
  @Test
  @Owner(developers = OwnerRule.BOJAN)
  @Category(UnitTests.class)
  public void testFilterEnumFFDisabled() {
    doReturn(false).when(featureFlagHelper).isEnabled(any(), any());
    List<WeekdaysEnum> types = featureFlagFilterService.filterEnum(
        "accountId", FeatureName.SSH_NG, WeekdaysEnum.class, new TestFeatureFlagFilterContext());

    assertThat(types.size()).isEqualTo(5);
  }
}
