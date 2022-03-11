package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.validator.dto.HostValidationDTO;
import io.harness.ng.validator.service.api.HostValidationService;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(HarnessTeam.PL)
public class HostValidationResourceTest extends CategoryTest {
  @Mock HostValidationService hostValidationService;
  @InjectMocks HostValidationResource hostValidationResource;

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldValidateSshHosts() {
    String accountIdentifier = "account1";
    String secretIdentifier = "sshSecret";
    String host1 = "host1";
    List<String> hostNames = Arrays.asList(host1);
    HostValidationDTO hostValidationDTO =
        HostValidationDTO.builder().host(host1).status(HostValidationDTO.HostValidationStatus.SUCCESS).build();
    doReturn(Arrays.asList(hostValidationDTO))
        .when(hostValidationService)
        .validateSSHHosts(hostNames, accountIdentifier, null, null, secretIdentifier);
    ResponseDTO<List<HostValidationDTO>> result =
        hostValidationResource.validateSshHost(accountIdentifier, null, null, secretIdentifier, Arrays.asList(host1));
    assertThat(result.getData().get(0).getHost()).isEqualTo(host1);
    assertThat(result.getData().get(0).getStatus()).isEqualTo(HostValidationDTO.HostValidationStatus.SUCCESS);
    assertThat(result.getData().get(0).getError()).isNull();
  }
}
