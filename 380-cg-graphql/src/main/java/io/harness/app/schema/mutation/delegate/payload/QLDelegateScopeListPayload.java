package io.harness.app.schema.mutation.delegate.payload;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import io.harness.app.schema.type.delegate.QLDelegateScope;
import lombok.Singular;
import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.DELEGATE)
public class QLDelegateScopeListPayload implements QLObject {
  String clientMutationId;
  @Singular
  private List<QLDelegateScope> scopes;
}
