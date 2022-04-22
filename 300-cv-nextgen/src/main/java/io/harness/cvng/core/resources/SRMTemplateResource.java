/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.beans.template.SRMTemplateDTO;
import io.harness.cvng.core.beans.template.TemplateType;
import io.harness.cvng.core.services.api.SRMTemplateService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.sun.istack.internal.NotNull;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import retrofit2.http.Body;

@Api("template/")
@Path("template")
@Produces("application/json")
@NextGenManagerAuth
public class SRMTemplateResource {
  @Inject SRMTemplateService srmTemplateService;

  @GET
  @Timed
  @ExceptionMetered
  @Path("/list")
  @ApiOperation(value = "List SRM templates", nickname = "ListSRMTemplates")
  public ResponseDTO<PageResponse<SRMTemplateDTO>> listTemplates(@NotNull @Valid @BeanParam ProjectParams projectParams,
      @QueryParam("templateType") List<TemplateType> templateTypes, @QueryParam("searchString") String searchString,
      @NotNull @Valid @BeanParam PageRequest pageRequest) {
    return ResponseDTO.newResponse(srmTemplateService.list(projectParams, templateTypes, searchString, pageRequest));
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/{fullyQualifiedIdentfier}")
  @ApiOperation(value = "Get a SRM template by fully Qualified Identifier", nickname = "getSRMTemplateByIdentifier")
  public ResponseDTO<SRMTemplateDTO> get(@NotNull @Valid @QueryParam("accountId") String accountId,
      @NotNull @PathParam("fullyQualifiedIdentfier") String fullyQualifiedIdentifier) {
    return ResponseDTO.newResponse(srmTemplateService.get(accountId, fullyQualifiedIdentifier));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/")
  @ApiOperation(value = "Create SRM template", nickname = "createSRMTemplate")
  public ResponseDTO<SRMTemplateDTO> create(
      @NotNull @Valid @BeanParam ProjectParams projectParams, @NotNull @Valid @Body SRMTemplateDTO srmTemplateDTO) {
    return ResponseDTO.newResponse(srmTemplateService.create(projectParams, srmTemplateDTO));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("/{fullyQualifiedIdentfier}")
  @ApiOperation(value = "Get a SRM template by fully Qualified Identifier", nickname = "getSRMTemplateByIdentifier")
  public ResponseDTO<SRMTemplateDTO> update(@NotNull @Valid @BeanParam ProjectParams projectParams,
      @NotNull @PathParam("fullyQualifiedIdentfier") String fullyQualifiedIdentifier,
      @NotNull @Valid @Body SRMTemplateDTO srmTemplateDTO) {
    return ResponseDTO.newResponse(srmTemplateService.update(projectParams, fullyQualifiedIdentifier, srmTemplateDTO));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("/{fullyQualifiedIdentfier}")
  @ApiOperation(
      value = "Delete a SRM template by fully Qualified Identifier", nickname = "deleteSRMTemplateByIdentifier")
  public ResponseDTO<Boolean>
  delete(@NotNull @Valid @QueryParam("accountId") String accountId,
      @NotNull @PathParam("fullyQualifiedIdentfier") String fullyQualifiedIdentifier) {
    return ResponseDTO.newResponse(srmTemplateService.delete(accountId, fullyQualifiedIdentifier));
  }
}
