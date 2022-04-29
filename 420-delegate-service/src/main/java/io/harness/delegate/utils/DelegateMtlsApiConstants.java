/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

/**
 * Contains common constants for the delegate mTLS REST API across CG / NG Apis.
 */
@UtilityClass
@OwnedBy(DEL)
public class DelegateMtlsApiConstants {
  // mTLS delegate related REST api entry points (relative and absolute)
  public static final String API_ROOT_RELATIVE = "delegate-mtls";
  public static final String API_ROOT_RELATIVE_NG_INTERNAL = API_ROOT_RELATIVE + "/ng";

  public static final String API_ROOT = "/" + API_ROOT_RELATIVE;
  public static final String API_ROOT_NG_INTERNAL = "/" + API_ROOT_RELATIVE_NG_INTERNAL;

  public static final String API_PATH_ENDPOINT = "endpoint";
  public static final String API_PATH_CHECK_AVAILABILITY = "check-availability";

  // Common parameter names and descriptions.
  public static final String API_PARAM_DOMAIN_PREFIX_NAME = "domainPrefix";
  public static final String API_PARAM_DOMAIN_PREFIX_DESC = "The domain prefix to check.";

  public static final String API_PARAM_CREATE_REQUEST_DESC = "The details of the delegate mTLS endpoint to create.";
  public static final String API_PARAM_UPDATE_REQUEST_DESC = "The details to update for the delegate mTLS endpoint.";
  public static final String API_PARAM_PATCH_REQUEST_DESC =
      "A subset of the details to update for the delegate mTLS endpoint.";

  // Common operation names and descriptions
  public static final String API_OPERATION_ENDPOINT_CREATE_NAME = "createDelegateMtlsEndpointForAccount";
  public static final String API_OPERATION_ENDPOINT_CREATE_DESC = "Creates the delegate mTLS endpoint for an account.";

  public static final String API_OPERATION_ENDPOINT_UPDATE_NAME = "updateDelegateMtlsEndpointForAccount";
  public static final String API_OPERATION_ENDPOINT_UPDATE_DESC =
      "Updates the existing delegate mTLS endpoint for an account.";

  public static final String API_OPERATION_ENDPOINT_PATCH_NAME = "patchDelegateMtlsEndpointForAccount";
  public static final String API_OPERATION_ENDPOINT_PATCH_DESC =
      "Updates selected properties of the existing delegate mTLS endpoint for an account.";

  public static final String API_OPERATION_ENDPOINT_GET_NAME = "getDelegateMtlsEndpointForAccount";
  public static final String API_OPERATION_ENDPOINT_GET_DESC = "Gets the delegate mTLS endpoint for an account.";

  public static final String API_OPERATION_ENDPOINT_DELETE_NAME = "deleteDelegateMtlsEndpointForAccount";
  public static final String API_OPERATION_ENDPOINT_DELETE_DESC = "Removes the delegate mTLS endpoint for an account.";

  public static final String API_OPERATION_ENDPOINT_CHECK_AVAILABILITY_NAME =
      "checkDelegateMtlsEndpointDomainPrefixAvailability";
  public static final String API_OPERATION_ENDPOINT_CHECK_AVAILABILITY_DESC =
      "Checks whether a given delegate mTLS endpoint domain prefix is available.";
}
