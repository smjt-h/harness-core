package io.harness.ng.core.delegate.client;

import io.harness.NGCommonEntityConstants;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroupTags;
import io.harness.rest.RestResponse;

import javax.validation.constraints.NotNull;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DelegateConfigClient {

  String DELEGATE_SETUP_API = "/setup/delegates/ng/v2";

  @PUT(DELEGATE_SETUP_API + "/{" + NGCommonEntityConstants.IDENTIFIER_KEY + "}/tags")
  Call<RestResponse<DelegateGroup>> updateDelegateGroupTags(
      @Path(NGCommonEntityConstants.IDENTIFIER_KEY) @NotNull String groupIdentifier,
      @Query("accountId") @NotNull String accountIdentifier,
//      @Query("orgId") String orgIdentifier,
//      @Query("projectId") String projectIdentifier,
      @Body @NotNull DelegateGroupTags tags);
}