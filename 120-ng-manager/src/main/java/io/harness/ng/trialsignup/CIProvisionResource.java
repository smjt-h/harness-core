/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.trialsignup;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions.EDIT_CONNECTOR_PERMISSION;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.ACCOUNT;
import static io.harness.delegate.beans.connector.scm.GitConnectionType.REPO;
import static io.harness.delegate.beans.connector.scm.github.GithubApiAccessType.TOKEN;
import static io.harness.exception.WingsException.USER;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.connector.helper.ConnectorRbacHelper;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.github.GithubTokenSpecDTO;
import io.harness.encryption.SecretRefData;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.interceptor.GitEntityCreateInfoDTO;
import io.harness.ng.NextGenModule;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretRequestWrapper;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.service.ScmClient;

import software.wings.beans.GitConfig;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(CI)
@Api("trial-signup")
@Path("/trial-signup")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
//@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class CIProvisionResource {
  @Data
  @Builder
  @Schema(name = "Connector", description = "Connector")
  public static class ScmConnectorDTO implements YamlDTO {
    @JsonProperty("secret")  SecretDTOV2 secret;
    @JsonProperty("connector") ConnectorInfoDTO connectorInfo;

  }

  @Data
  @Builder
  @Schema(name = "ScmConnectorDTOResponse", description = "ScmConnectorDTOResponse")
  public static class ScmConnectorDTOResponse {
    ConnectorResponseDTO connectorResponseDTO;
    SecretResponseWrapper secretResponseWrapper;
    ConnectorValidationResult connectorValidationResult;
  }

  private static final String GITHUB_SCM_CONNECTOR_NAME = "Github";
  private static final String GITHUB_SCM_CONNECTOR_IDENTIFIER = "HARNESS_GITHUB_SCM_CONNECTOR";
  @Inject ProvisionService provisionService;
  @Inject(optional = true) private ScmClient scmClient;
  @Inject @Named(value = DEFAULT_CONNECTOR_SERVICE) private ConnectorService connectorService;
  @Inject private SecretCrudService ngSecretService;
  @Inject private AccessControlClient accessControlClient;

  @PUT
  @Path("provision")
  @ApiOperation(value = "Provision resources for signup", nickname = "provisionResourcesForCI")
  public ResponseDTO<ProvisionResponse.SetupStatus> provisionCIResources(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return ResponseDTO.newResponse(provisionService.provisionCIResources(accountId));
  }

  @GET
  @Path("delegate-install-status")
  @ApiOperation(value = "Provision resources for signup", nickname = "getDelegateInstallStatus")
  public ResponseDTO<ProvisionResponse.DelegateStatus> getDelegateInstallStatus(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return ResponseDTO.newResponse(provisionService.getDelegateInstallStatus(accountId));
  }

  @POST
  @Path("create-scm")
  @ApiOperation(value = "Creates default scm Connector", nickname = "createDefaultScmConnector")
  public ResponseDTO<ScmConnectorDTOResponse>
  createDefaultScm(@RequestBody(required = true, description = "Details of the Connector to create") @Valid
                   @NotNull ScmConnectorDTO scmConnectorDTO,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier) {

    ConnectorResponseDTO connectorResponseDTO = null;
    SecretResponseWrapper secretResponseWrapper = null;

    SecretDTOV2 secretDTOV2 = scmConnectorDTO.getSecret();

    Optional<SecretResponseWrapper> secretResponseWrapperOptional =
            ngSecretService.get(accountIdentifier, null, null, secretDTOV2.getIdentifier());

    if (secretResponseWrapperOptional.isPresent()) {
      secretResponseWrapper = ngSecretService.update(accountIdentifier, null, null, secretDTOV2.getIdentifier(), secretDTOV2);
    } else {
      secretResponseWrapper =
              ngSecretService.create(accountIdentifier, secretDTOV2);
    }
    
    ConnectorInfoDTO connectorInfoDTO = scmConnectorDTO.getConnectorInfo();

    Optional<ConnectorResponseDTO> connectorResponseDTOOptional =
        connectorService.get(accountIdentifier, null, null, connectorInfoDTO.getIdentifier());

    if (!connectorResponseDTOOptional.isPresent()) {
      connectorResponseDTO =
          connectorService.create(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }
    else
    {
      connectorService.update(ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build(), accountIdentifier);
    }



    ConnectorValidationResult connectorValidationResult = connectorService.testConnection(accountIdentifier, null, null, connectorInfoDTO.getIdentifier());

    return ResponseDTO.newResponse(ScmConnectorDTOResponse.builder()
                                       .connectorResponseDTO(connectorResponseDTO)
                                       .secretResponseWrapper(secretResponseWrapper)
                                        .connectorValidationResult(connectorValidationResult)
                                       .build());
  }
}
