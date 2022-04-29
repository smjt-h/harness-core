/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.envGroup.resource;

import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.pms.rbac.NGResourceType.ENVIRONMENT;
import static io.harness.utils.PageUtils.getNGPageResponse;

import static java.lang.Long.parseLong;
import static javax.ws.rs.core.HttpHeaders.IF_MATCH;
import static org.apache.commons.lang3.StringUtils.isNumeric;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.OrgIdentifier;
import io.harness.accesscontrol.ProjectIdentifier;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity.EnvironmentGroupKeys;
import io.harness.cdng.envGroup.beans.EnvironmentGroupFilterPropertiesDTO;
import io.harness.cdng.envGroup.mappers.EnvironmentGroupMapper;
import io.harness.cdng.envGroup.services.EnvironmentGroupService;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.gitsync.interceptor.GitEntityDeleteInfoDTO;
import io.harness.gitsync.interceptor.GitEntityFindInfoDTO;
import io.harness.gitsync.interceptor.GitEntityUpdateInfoDTO;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupDeleteResponse;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponse;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.mappers.EnvironmentMapper;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.pms.rbac.NGResourceType;
import io.harness.rbac.CDNGRbacPermissions;
import io.harness.security.annotations.NextGenManagerAuth;
import io.harness.utils.PageUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@NextGenManagerAuth
@Api("/environmentGroup")
@Path("/environmentGroup")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
// TODO: Need to uncomment this for customer to use the apis: @Tag(name = "EnvironmentGroup", description = "This
// contains APIs related to EnvironmentGroup")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.BAD_REQUEST_CODE,
    description = NGCommonEntityConstants.BAD_REQUEST_PARAM_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = FailureDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_CODE,
    description = NGCommonEntityConstants.INTERNAL_SERVER_ERROR_MESSAGE,
    content =
    {
      @Content(mediaType = NGCommonEntityConstants.APPLICATION_JSON_MEDIA_TYPE,
          schema = @Schema(implementation = ErrorDTO.class))
      ,
          @Content(mediaType = NGCommonEntityConstants.APPLICATION_YAML_MEDIA_TYPE,
              schema = @Schema(implementation = ErrorDTO.class))
    })
@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class EnvironmentGroupResource {
  private final EnvironmentGroupService environmentGroupService;
  private final EnvironmentService environmentService;
  private final AccessControlClient accessControlClient;
  public static final String ENVIRONMENT_GROUP_PARAM_MESSAGE = "Environment Group Identifier for the entity";

  @GET
  @Path("{envGroupIdentifier}")
  @ApiOperation(value = "Gets a Environment Group by identifier", nickname = "getEnvironmentGroup")
  @Operation(operationId = "getEnvironmentGroup", summary = "Gets an Environment Group by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The saved Environment Group")
      })
  @NGAccessControlCheck(resourceType = NGResourceType.ENVIRONMENT_GROUP,
      permission = CDNGRbacPermissions.ENVIRONMENT_GROUP_VIEW_PERMISSION)
  public ResponseDTO<EnvironmentGroupResponse>
  get(@Parameter(description = ENVIRONMENT_GROUP_PARAM_MESSAGE) @NotNull @PathParam(
          NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) @ResourceIdentifier String envGroupId,
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @Parameter(description = "Specify whether Environment is deleted or not") @QueryParam(
          NGCommonEntityConstants.DELETED_KEY) @DefaultValue("false") boolean deleted,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    Optional<EnvironmentGroupEntity> environmentGroupEntity =
        environmentGroupService.get(accountId, orgIdentifier, projectIdentifier, envGroupId, deleted);

    // fetching Environments from list of identifiers
    if (environmentGroupEntity.isPresent()) {
      List<EnvironmentResponse> envResponseList = getEnvironmentResponses(environmentGroupEntity.get());
      return ResponseDTO.newResponse(
          EnvironmentGroupMapper.toResponseWrapper(environmentGroupEntity.get(), envResponseList));
    }

    return null;
  }

  @POST
  @ApiOperation(value = "Create an Environment Group", nickname = "createEnvironmentGroup")
  @Operation(operationId = "postEnvironmentGroup", summary = "Create an Environment Group",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "If the YAML is valid, returns created Environment Group. If not, it sends what is wrong with the YAML")
      })
  @NGAccessControlCheck(resourceType = NGResourceType.ENVIRONMENT_GROUP,
      permission = CDNGRbacPermissions.ENVIRONMENT_GROUP_CREATE_PERMISSION)
  public ResponseDTO<EnvironmentGroupResponse>
  create(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @Parameter(
             description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @Parameter(
          description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier @Parameter(
          description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier,
      @RequestBody(required = true,
          description =
              "Environment Group YAML to be created. The Account, Org,  and Project identifiers inside the YAML should match the query parameters.")
      @NotNull String yaml,
      @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    EnvironmentGroupEntity entity =
        EnvironmentGroupMapper.toEnvironmentEntity(accountId, orgIdentifier, projectIdentifier, yaml);
    // Validate the fields of the Entity
    validate(entity);

    // validate view permissions for each environment linked with environment group
    validatePermissionForEnvironment(entity);

    EnvironmentGroupEntity savedEntity = environmentGroupService.create(entity);

    // fetching Environments from list of identifiers
    List<EnvironmentResponse> envResponseList = null;
    envResponseList = getEnvironmentResponses(savedEntity);

    return ResponseDTO.newResponse(EnvironmentGroupMapper.toResponseWrapper(savedEntity, envResponseList));
  }

  @POST
  @Path("/list")
  @ApiOperation(value = "Gets Environment Group list", nickname = "getEnvironmentGroupList")
  @Operation(operationId = "getEnvironmentGroupList", summary = "Gets Environment Group list for a Project",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of Environment Group for a Project")
      })
  public ResponseDTO<PageResponse<EnvironmentGroupResponse>>
  listEnvironmentGroup(@Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
                           NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ResourceIdentifier String projectIdentifier,
      @QueryParam("envGroupIdentifiers") List<String> envGroupIds,
      @Parameter(description = "The word to be searched and included in the list response") @QueryParam(
          NGResourceFilterConstants.SEARCH_TERM_KEY) String searchTerm,
      @Parameter(description = NGCommonEntityConstants.PAGE_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PAGE) @DefaultValue("0") int page,
      @Parameter(description = NGCommonEntityConstants.SIZE_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.SIZE) @DefaultValue("25") int size,
      @QueryParam("sort") @Parameter(description = NGCommonEntityConstants.SORT_PARAM_MESSAGE) List<String> sort,
      @Parameter(description = "Filter identifier") @QueryParam(
          NGResourceFilterConstants.FILTER_KEY) String filterIdentifier,
      @RequestBody(description = "This is the body for the filter properties for listing Environment Groups")
      FilterPropertiesDTO filterProperties, @BeanParam GitEntityFindInfoDTO gitEntityBasicInfo) {
    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgIdentifier, projectIdentifier),
        Resource.of(NGResourceType.ENVIRONMENT_GROUP, null), CDNGRbacPermissions.ENVIRONMENT_GROUP_VIEW_PERMISSION);
    Criteria criteria = environmentGroupService.formCriteria(accountId, orgIdentifier, projectIdentifier, false,
        searchTerm, filterIdentifier, (EnvironmentGroupFilterPropertiesDTO) filterProperties);

    if (EmptyPredicate.isNotEmpty(envGroupIds)) {
      criteria.and(EnvironmentGroupKeys.identifier).in(envGroupIds);
    }
    Pageable pageRequest =
        PageUtils.getPageRequest(page, size, sort, Sort.by(Sort.Direction.DESC, EnvironmentGroupKeys.lastModifiedAt));

    Page<EnvironmentGroupEntity> envGroupEntities =
        environmentGroupService.list(criteria, pageRequest, projectIdentifier, orgIdentifier, accountId);

    return ResponseDTO.newResponse(getNGPageResponse(envGroupEntities.map(
        envGroup -> EnvironmentGroupMapper.toResponseWrapper(envGroup, getEnvironmentResponses(envGroup)))));
  }

  @DELETE
  @Path("{envGroupIdentifier}")
  @ApiOperation(value = "Delete en Environment Group by Identifier", nickname = "deleteEnvironmentGroup")
  @Operation(operationId = "deleteEnvironmentGroup", summary = "Delete en Environment Group by Identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns true if the Environment Group is deleted")
      })
  @NGAccessControlCheck(resourceType = NGResourceType.ENVIRONMENT_GROUP,
      permission = CDNGRbacPermissions.ENVIRONMENT_GROUP_DELETE_PERMISSION)
  public ResponseDTO<EnvironmentGroupDeleteResponse>
  delete(@HeaderParam(IF_MATCH) String ifMatch,
      @Parameter(description = ENVIRONMENT_GROUP_PARAM_MESSAGE) @PathParam(
          NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) @ResourceIdentifier String envGroupId,
      @Parameter(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountId,
      @Parameter(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @OrgIdentifier String orgIdentifier,
      @Parameter(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) @NotNull @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier String projectIdentifier,
      @BeanParam GitEntityDeleteInfoDTO entityDeleteInfo) {
    // TODO: set up usages of env group as well as env linked with it
    log.info(String.format("Delete Environment group Api %s", envGroupId));
    EnvironmentGroupEntity deletedEntity = environmentGroupService.delete(
        accountId, orgIdentifier, projectIdentifier, envGroupId, isNumeric(ifMatch) ? parseLong(ifMatch) : null);
    return ResponseDTO.newResponse(EnvironmentGroupMapper.toDeleteResponseWrapper(deletedEntity));
  }

  @PUT
  @Path("/{envGroupIdentifier}")
  @ApiOperation(value = "Update an Environment Group by Identifier", nickname = "updateEnvironmentGroup")
  @Operation(operationId = "updateEnvironmentGroup", summary = "Update an Environment Group by Identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the updated Environment Group")
      })
  @NGAccessControlCheck(resourceType = NGResourceType.ENVIRONMENT_GROUP,
      permission = CDNGRbacPermissions.ENVIRONMENT_GROUP_UPDATE_PERMISSION)
  public ResponseDTO<EnvironmentGroupResponse>
  update(@HeaderParam(IF_MATCH) String ifMatch,
      @Parameter(description = ENVIRONMENT_GROUP_PARAM_MESSAGE) @NotNull @PathParam(
          NGCommonEntityConstants.ENVIRONMENT_GROUP_KEY) @ResourceIdentifier String envGroupId,
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @Parameter(
          description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) String accountId,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) @OrgIdentifier @Parameter(
          description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) @ProjectIdentifier @Parameter(
          description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) String projectIdentifier,
      @RequestBody(required = true,
          description =
              "Environment Group YAML to be updated. The Account, Org, and Project identifiers inside the YAML should match the query parameters.")
      @NotNull String yaml,
      @BeanParam GitEntityUpdateInfoDTO gitEntityInfo) {
    log.info(String.format("Updating Environment Group with identifier %s in project %s, org %s, account %s",
        envGroupId, projectIdentifier, orgIdentifier, accountId));
    EnvironmentGroupEntity requestedEntity =
        EnvironmentGroupMapper.toEnvironmentEntity(accountId, orgIdentifier, projectIdentifier, yaml);
    // Validate the fields of the Entity
    validate(requestedEntity);

    // validate view permissions for each environment linked with environment group
    validatePermissionForEnvironment(requestedEntity);

    // Validating if identifier is same passed in yaml and path param
    if (!envGroupId.equals(requestedEntity.getIdentifier())) {
      throw new InvalidRequestException("Updating of Environment Group Identifier is not supported");
    }

    // updating the version if any
    requestedEntity.setVersion(isNumeric(ifMatch) ? parseLong(ifMatch) : null);

    EnvironmentGroupEntity updatedEntity = environmentGroupService.update(requestedEntity);

    // fetching Environments from list of identifiers
    List<EnvironmentResponse> envResponseList = getEnvironmentResponses(updatedEntity);

    return ResponseDTO.newResponse(updatedEntity.getVersion().toString(),
        EnvironmentGroupMapper.toResponseWrapper(updatedEntity, envResponseList));
  }

  @VisibleForTesting
  List<EnvironmentResponse> getEnvironmentResponses(EnvironmentGroupEntity groupEntity) {
    List<EnvironmentResponse> envResponseList = null;

    List<Environment> envList =
        environmentService.fetchesNonDeletedEnvironmentFromListOfIdentifiers(groupEntity.getAccountId(),
            groupEntity.getOrgIdentifier(), groupEntity.getProjectIdentifier(), groupEntity.getEnvIdentifiers());
    envResponseList = EnvironmentMapper.toResponseWrapper(envList);

    return envResponseList;
  }

  @VisibleForTesting
  void validatePermissionForEnvironment(EnvironmentGroupEntity envGroup) {
    String accountId = envGroup.getAccountId();
    String orgId = envGroup.getOrgIdentifier();
    String projectId = envGroup.getProjectIdentifier();

    List<String> envIdentifiers = envGroup.getEnvIdentifiers();
    envIdentifiers.forEach(envId
        -> accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, orgId, projectId),
            Resource.of(ENVIRONMENT, envId), CDNGRbacPermissions.ENVIRONMENT_VIEW_PERMISSION));
  }
}
