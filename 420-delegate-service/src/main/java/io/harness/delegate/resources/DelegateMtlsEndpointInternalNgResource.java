/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;
import io.harness.delegate.utils.DelegateMtlsApiConstants;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.v3.oas.annotations.Hidden;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes the internal delegate mTLS endpoint management REST Api on CG which is called by NG.
 */
@Path(DelegateMtlsApiConstants.API_ROOT_NG_INTERNAL)
@Produces("application/json")
@Consumes("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Hidden
public class DelegateMtlsEndpointInternalNgResource {
  private final DelegateMtlsEndpointService delegateMtlsEndpointService;

  @Inject
  public DelegateMtlsEndpointInternalNgResource(DelegateMtlsEndpointService delegateMtlsEndpointService) {
    this.delegateMtlsEndpointService = delegateMtlsEndpointService;
  }

  @POST
  @Path(DelegateMtlsApiConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> createEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.createEndpointForAccount(accountIdentifier, endpointRequest));
    }
  }

  @PUT
  @Path(DelegateMtlsApiConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> updateEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.updateEndpointForAccount(accountIdentifier, endpointRequest));
    }
  }

  @PATCH
  @Path(DelegateMtlsApiConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> patchEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest patchRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.patchEndpointForAccount(accountIdentifier, patchRequest));
    }
  }

  @DELETE
  @Path(DelegateMtlsApiConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<Boolean> deleteEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(this.delegateMtlsEndpointService.deleteEndpointForAccount(accountIdentifier));
    }
  }

  @GET
  @Path(DelegateMtlsApiConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> getEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(this.delegateMtlsEndpointService.getEndpointForAccount(accountIdentifier));
    }
  }

  /**
   * Checks whether the provided domain prefix is available.
   *
   * @param accountIdentifier required to be compliant with new internal OpenAPI specifications.
   * @param domainPrefix The domain prefix to check.
   * @return True if and only if there is no existing delegate mTLS endpoint that uses the provided domain prefix.
   */
  @GET
  @Path(DelegateMtlsApiConstants.API_PATH_CHECK_AVAILABILITY)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<Boolean> isDomainPrefixAvailable(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @QueryParam(DelegateMtlsApiConstants.API_PARAM_DOMAIN_PREFIX_NAME) @NotNull String domainPrefix) {
    return new RestResponse<>(this.delegateMtlsEndpointService.isDomainPrefixAvailable(domainPrefix));
  }
}
