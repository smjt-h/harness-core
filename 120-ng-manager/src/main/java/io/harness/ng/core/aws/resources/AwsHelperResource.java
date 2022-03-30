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
import io.harness.connector.ConnectorInfoDTO;
import io.harness.ng.NextGenConfiguration;
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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;

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
  @Inject NextGenConfiguration configuration;

  @GET
  @Path("regions")
  @ApiOperation(value = "Get all the AWS regions defined in the application", nickname = "RegionsForAwsHelper")
  public ResponseDTO<Map<String, String>> listRegions() {
    return ResponseDTO.newResponse(configuration.getAwsRegionIdToName());
  }

  @POST
  @Path("cf-paramters")
  @ApiOperation(value = "", nickname = "")
  public ResponseDTO<List<AwsCFTemplateParamsData>> listCFParameterKeys(
          @QueryParam("type") @NotEmpty String type,
          @QueryParam("region") @NotEmpty String region,
          @QueryParam("sourceRepoSettingId") String sourceRepoSettingId,
          @QueryParam("path") String templatePath,
          @QueryParam("commitId") String commitId,
          @QueryParam("branch") String sourceRepoBranch,
          @QueryParam("useBranch") Boolean useBranch,
          @QueryParam("repoName") String repoName,
          @QueryParam("connectorRef") @NotNull  String awsConnectorRef,
          @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
          @QueryParam(NGCommonEntityConstants.ORG_KEY) @NotNull String orgIdentifier,
          @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @NotNull String projectIdentifier,
          String data
  ) {
    IdentifierRef connectorRef =
            IdentifierRefHelper.getIdentifierRef(awsConnectorRef, accountIdentifier, orgIdentifier, projectIdentifier);


    List<AwsCFTemplateParamsData> keys = awsHelperService.awsCFParameterKeys(
            type,
            region,
            sourceRepoSettingId,
            templatePath,
            commitId,
            sourceRepoBranch,
            connectorRef,
            orgIdentifier,
            projectIdentifier,
            data,
            connectorDTO
    );

    return ResponseDTO.newResponse(keys);
  }



  @GET
  @Path("cf-capabilities")
  @ApiOperation(value = "Get the Cloudformation capabilities", nickname = "CFCapabilitiesForAwsHelper")
  public ResponseDTO<List<String>> listCFCapabilities() {
    return ResponseDTO.newResponse(awsHelperService.getCapabilities());
  }

  @GET
  @Path("cf-states")
  @ApiOperation(value = "Get all the Cloudformation states for a stack", nickname = "CFStatesForAwsHelper")
  public ResponseDTO<Set<String>> listCFStates() {
    return ResponseDTO.newResponse(awsHelperService.getCFStates());
  }
}
