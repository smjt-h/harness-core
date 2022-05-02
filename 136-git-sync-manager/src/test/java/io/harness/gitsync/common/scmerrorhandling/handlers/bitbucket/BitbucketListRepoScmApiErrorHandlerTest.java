package io.harness.gitsync.common.scmerrorhandling.handlers.bitbucket;

import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.category.element.UnitTests;
import io.harness.exception.HintException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class BitbucketListRepoScmApiErrorHandlerTest extends GitSyncTestBase {
  @Inject BitbucketListRepoScmApiErrorHandler bitbucketListRepoScmApiErrorHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testHandleError() {
    assertThatThrownBy(() -> bitbucketListRepoScmApiErrorHandler.handleError(401, "Not Authorised"))
        .isInstanceOf(HintException.class);
  }
}
