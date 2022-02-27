package io.harness.execution;

import com.google.inject.Inject;
import com.jcraft.jsch.ConfigRepository;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.repositories.CIExecutionConfigRepository;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.Optional;

import static io.harness.rule.OwnerRule.AMAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Slf4j
public class CIExecutionConfigServiceTest extends CIExecutionTestBase {
    @Mock CIExecutionConfigRepository cIExecutionConfigRepository;
    @Inject CIExecutionConfigService ciExecutionConfigService;

    @Test
    @Owner(developers = AMAN)
    @Category(UnitTests.class)
    public void getAddonImageTest() {
        CIExecutionConfig executionConfig = CIExecutionConfig.builder().accountIdentifier("acct").buildAndPushDockerRegistryImage("dockerImage").addOnImage("addon:1,3.4").liteEngineImage("le:1,4.4").build();
        when(cIExecutionConfigRepository.findFirstByAccountIdentifier("acct")).thenReturn(Optional.of(executionConfig));
        String addonImage = ciExecutionConfigService.getAddonImage("acct");
        assertThat(addonImage).isEqualTo("addon:1,3.4");
    }

}