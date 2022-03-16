/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.exception.WingsException.USER;
import static io.harness.ng.accesscontrol.PlatformPermissions.VIEW_USERGROUP_PERMISSION;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import io.harness.CategoryTest;
import io.harness.accesscontrol.NGAccessDeniedException;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.validator.dto.HostValidationDTO;
import io.harness.ng.validator.service.api.HostValidationService;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@OwnedBy(HarnessTeam.CDP)
public class HostValidationResourceTest extends CategoryTest {
  private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  private static final String PROJECT_IDENTIFIER = "projectIdentifier";
  private static final String SECRET_IDENTIFIER = "secretIdentifier";

  @Mock HostValidationService hostValidationService;
  @Mock AccessControlClient accessControlClient;
  @InjectMocks HostValidationResource hostValidationResource;

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldValidateSshHosts() {
    String accountIdentifier = "account1";
    String host1 = "host1";
    List<String> hosts = Collections.singletonList(host1);
    HostValidationDTO hostValidationDTO =
        HostValidationDTO.builder().host(host1).status(HostValidationDTO.HostValidationStatus.SUCCESS).build();
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(any(ResourceScope.class), any(Resource.class), eq(VIEW_USERGROUP_PERMISSION));
    doReturn(Collections.singletonList(hostValidationDTO))
        .when(hostValidationService)
        .validateSSHHosts(hosts, accountIdentifier, null, null, SECRET_IDENTIFIER);

    ResponseDTO<List<HostValidationDTO>> result = hostValidationResource.validateSshHost(
        accountIdentifier, null, null, SECRET_IDENTIFIER, Collections.singletonList(host1));

    assertThat(result.getData().get(0).getHost()).isEqualTo(host1);
    assertThat(result.getData().get(0).getStatus()).isEqualTo(HostValidationDTO.HostValidationStatus.SUCCESS);
    assertThat(result.getData().get(0).getError()).isNull();
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testValidateSshHostsWithException() {
    final List<String> hosts = Collections.singletonList("host");
    doNothing()
        .when(accessControlClient)
        .checkForAccessOrThrow(any(ResourceScope.class), any(Resource.class), eq(VIEW_USERGROUP_PERMISSION));
    doThrow(new InvalidRequestException("Secret identifier is empty or null"))
        .when(hostValidationService)
        .validateSSHHosts(hosts, ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER);

    assertThatThrownBy(()
                           -> hostValidationResource.validateSshHost(
                               ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER, hosts))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Secret identifier is empty or null");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testValidateSshHostsWithNGAccessDeniedException() {
    final List<String> hosts = Collections.singletonList("host");
    doThrow(new NGAccessDeniedException("Not enough permission", USER, Collections.emptyList()))
        .when(accessControlClient)
        .checkForAccessOrThrow(any(ResourceScope.class), any(Resource.class), eq(VIEW_USERGROUP_PERMISSION));

    assertThatThrownBy(()
                           -> hostValidationResource.validateSshHost(
                               ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, SECRET_IDENTIFIER, hosts))
        .isInstanceOf(NGAccessDeniedException.class)
        .hasMessage("Not enough permission");
  }
}
