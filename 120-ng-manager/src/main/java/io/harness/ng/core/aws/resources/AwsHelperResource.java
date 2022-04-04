/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.aws.resources;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.aws.service.AwsHelperResourceServiceImpl;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.utils.IdentifierRefHelper;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Api("aws")
@Path("/aws/aws-helper")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class AwsHelperResource {
  private final AwsHelperResourceServiceImpl awsHelperService;

  @GET
  @Path("regions")
  @ApiOperation(value = "Get all the AWS regions defined in the application", nickname = "RegionsForAws")
  public ResponseDTO<Map<String, String>> listRegions() {
    return ResponseDTO.newResponse(awsHelperService.getRegions());
  }

  @GET
  @Path("cf-capabilities")
  @ApiOperation(value = "Get the Cloudformation capabilities", nickname = "CFCapabilitiesForAws")
  public ResponseDTO<List<String>> listCFCapabilities() {
    return ResponseDTO.newResponse(awsHelperService.getCapabilities());
  }

  @GET
  @Path("cf-states")
  @ApiOperation(value = "Get all the Cloudformation states for a stack", nickname = "CFStatesForAws")
  public ResponseDTO<Set<String>> listCFStates() {
    return ResponseDTO.newResponse(awsHelperService.getCFStates());
  }

  @GET
  @Path("iam-roles")
  @ApiOperation(value = "Get all the IAM roles", nickname = "getIamRolesForAws")
  public ResponseDTO<Map<String, String>> listIamRoles(@NotNull @QueryParam("connectorRef") String awsConnectorRef,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier) {
    IdentifierRef connectorRef =
        IdentifierRefHelper.getIdentifierRef(awsConnectorRef, accountIdentifier, orgIdentifier, projectIdentifier);
    return ResponseDTO.newResponse(awsHelperService.getRolesARNs(connectorRef, orgIdentifier, projectIdentifier));
  }
}
