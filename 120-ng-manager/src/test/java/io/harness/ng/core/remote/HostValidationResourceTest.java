package io.harness.ng.core.remote;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.HostValidationResult;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.validator.HostValidationService;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static io.harness.rule.OwnerRule.VLAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(HarnessTeam.PL)
public class HostValidationResourceTest extends CategoryTest {

    @Mock
    HostValidationService hostValidationService;
    @InjectMocks
    HostValidationResource hostValidationResource;

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldValidateSshHosts() {
        String accountIdentifier = "account1";
        String secretIdentifier = "sshSecret";
        String host1 = "host1";
        List<String> hostNames = Arrays.asList(host1);
        HostValidationResult hostValidationResult = HostValidationResult.builder().host(host1).status(HostValidationResult.HostValidationStatus.SUCCESS).build();
        doReturn(Arrays.asList(hostValidationResult)).when(hostValidationService).validateSSHHosts(hostNames, accountIdentifier, null, null, secretIdentifier, Optional.empty());
        ResponseDTO<List<HostValidationResult>> result = hostValidationResource.validateSshHost(
                accountIdentifier,
                null,
                null,
                secretIdentifier,
                null,
                Arrays.asList(host1));
        assertThat(result.getData().get(0).getHost()).isEqualTo(host1);
        assertThat(result.getData().get(0).getStatus()).isEqualTo(HostValidationResult.HostValidationStatus.SUCCESS);
        assertThat(result.getData().get(0).getError()).isNull();
    }
}
