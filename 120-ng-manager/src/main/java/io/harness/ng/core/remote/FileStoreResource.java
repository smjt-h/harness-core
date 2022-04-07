/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.EntityIdentifier;
import io.harness.ng.core.api.FileStoreService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.security.annotations.NextGenManagerAuth;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.File;
import java.io.InputStream;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;

@OwnedBy(CDP)
@Path("/file-store")
@Api("/file-store")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@Tag(name = "File Store", description = "This contains APIs related to File Store in Harness")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@NextGenManagerAuth
@Slf4j
public class FileStoreResource {
  private final FileStoreService fileStoreService;

  @PUT
  @Consumes(MULTIPART_FORM_DATA)
  @Path("{identifier}")
  @ApiOperation(value = "Update file or folder", nickname = "update")
  @Operation(operationId = "update", summary = "Updates file or folder",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Returns update response") })
  public ResponseDTO<FileDTO>
  update(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @EntityIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) @EntityIdentifier(
          allowBlank = true) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @EntityIdentifier(allowBlank = true) String projectIdentifier,
      @Parameter(description = "The file identifier") @EntityIdentifier @PathParam("identifier") String identifier,
      @NotNull @Valid @BeanParam FileDTO file, @FormDataParam("content") InputStream content) {
    file.setAccountIdentifier(accountIdentifier);
    file.setOrgIdentifier(orgIdentifier);
    file.setProjectIdentifier(projectIdentifier);

    return ResponseDTO.newResponse(fileStoreService.update(file, content, identifier));
  }

  @POST
  @Consumes(MULTIPART_FORM_DATA)
  @ApiOperation(value = "Create file or folder", nickname = "create")
  @Operation(operationId = "create", summary = "Creates file or folder",
      responses = { @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "Returns create response") })
  public ResponseDTO<FileDTO>
  create(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @EntityIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) @EntityIdentifier(
          allowBlank = true) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @EntityIdentifier(allowBlank = true) String projectIdentifier,
      @FormDataParam("content") InputStream content, @NotNull @Valid @BeanParam FileDTO file) {
    file.setAccountIdentifier(accountIdentifier);
    file.setOrgIdentifier(orgIdentifier);
    file.setProjectIdentifier(projectIdentifier);

    return ResponseDTO.newResponse(fileStoreService.create(file, content));
  }

  @POST
  @Consumes({"application/json"})
  @Path("folder")
  @ApiOperation(value = "Get folder nodes", nickname = "getFolderNodes")
  @Operation(operationId = "getFolderNodes", summary = "Get Folder nodes.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of folder nodes as children")
      })
  public ResponseDTO<FolderNodeDTO>
  listFolderNodes(@Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                      NGCommonEntityConstants.ACCOUNT_KEY) @EntityIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) @EntityIdentifier(
          allowBlank = true) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @EntityIdentifier(allowBlank = true) String projectIdentifier,
      @RequestBody(required = true, description = "Folder node for which to return the list of nodes") @Valid
      @NotNull FolderNodeDTO folderNodeDTO) {
    return ResponseDTO.newResponse(
        fileStoreService.listFolderNodes(accountIdentifier, orgIdentifier, projectIdentifier, folderNodeDTO));
  }

  @GET
  @Path("file/{fileIdentifier}/download")
  @ApiOperation(value = "Download file", nickname = "downloadFile")
  @Operation(operationId = "downloadFile", summary = "Download File",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Download the file with content")
      })
  public Response
  downloadFile(@Parameter(description = "File identifier") @PathParam(
                   NGCommonEntityConstants.FILE_IDENTIFIER_KEY) @NotNull String fileIdentifier,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @EntityIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @QueryParam(NGCommonEntityConstants.ORG_KEY) @EntityIdentifier(
          allowBlank = true) String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @EntityIdentifier(allowBlank = true) String projectIdentifier) {
    File file = fileStoreService.downloadFile(accountIdentifier, orgIdentifier, projectIdentifier, fileIdentifier);
    return Response.ok(file, APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=" + file.getName())
        .build();
  }
}
