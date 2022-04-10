package io.harness.app.datafetcher.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.beans.SearchFilter.Operator.IN;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.schema.mutation.delegate.input.QLDelegateScopeListInput;
import io.harness.app.schema.mutation.delegate.payload.QLDelegateScopeListPayload;
import io.harness.app.schema.type.delegate.QLDelegateScope;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.delegate.beans.DelegateScope;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.DelegateScopeService;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(DEL)
public class DelegateScopesListDataFetcher
    extends BaseMutatorDataFetcher<QLDelegateScopeListInput, QLDelegateScopeListPayload> {
  @Inject DelegateScopeService delegateScopeService;

  @Inject
  public DelegateScopesListDataFetcher(DelegateScopeService delegateScopeService) {
    super(QLDelegateScopeListInput.class, QLDelegateScopeListPayload.class);
    this.delegateScopeService = delegateScopeService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.MANAGE_DELEGATES)
  protected QLDelegateScopeListPayload mutateAndFetch(
      QLDelegateScopeListInput parameter, MutationContext mutationContext) {
    PageRequest<DelegateScope> pageRequest = new PageRequest<>();
    pageRequest.addFilter(DelegateScope.ACCOUNT_ID_KEY, IN, parameter.getAccountId());
    pageRequest.setLimit(parameter.getLimit());
    pageRequest.setOffset(parameter.getOffset());
    PageResponse<DelegateScope> pageResponse = delegateScopeService.list(pageRequest);
    List<DelegateScope> scopes = pageResponse.getResponse();
    List<QLDelegateScope> delegateScopes =
        scopes.stream().map(DelegateController::populateQLDelegateScope).collect(Collectors.toList());
    return QLDelegateScopeListPayload.builder()
        .scopes(delegateScopes)
        .clientMutationId(parameter.getClientMutationId())
        .build();
  }
}
