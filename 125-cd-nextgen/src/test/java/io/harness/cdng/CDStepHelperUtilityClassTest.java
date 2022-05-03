package io.harness.cdng;

import static io.harness.rule.OwnerRule.VLICA;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.validation.Validator;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(PowerMockRunner.class)
@PrepareForTest({Validator.class})
public class CDStepHelperUtilityClassTest extends CategoryTest {
  @Test
  @Owner(developers = VLICA)
  @Category(UnitTests.class)
  public void testValidateGitStoreConfig() {
    CDStepHelper cDStepHelper = new CDStepHelper();

    ParameterField<String> branch = ParameterField.createValueField("branch");
    ParameterField<String> commit = ParameterField.createValueField("test-commit-id");

    GitStoreConfig gitStoreConfigBranch = GithubStore.builder().branch(branch).gitFetchType(FetchType.BRANCH).build();

    GitStoreConfig gitStoreConfigCommit = GithubStore.builder().commitId(commit).gitFetchType(FetchType.COMMIT).build();

    PowerMockito.mockStatic(Validator.class);
    PowerMockito.doNothing().when(Validator.class);

    cDStepHelper.validateGitStoreConfig(gitStoreConfigBranch);

    PowerMockito.verifyStatic(Validator.class, times(1));
    Validator.notEmptyCheck(eq("Branch is Empty in Git Store config"), eq("branch"));

    cDStepHelper.validateGitStoreConfig(gitStoreConfigCommit);

    PowerMockito.verifyStatic(Validator.class, times(1));
    Validator.notEmptyCheck(eq("Commit Id is Empty in Git Store config"), eq("test-commit-id"));
  }
}