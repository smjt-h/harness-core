package io.harness.ng.opa;

import static io.harness.security.dto.PrincipalType.USER;

import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.OpaUtils;
import io.harness.opaclient.model.OpaConstants;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.opaclient.model.OpaPolicyEvaluationResponse;
import io.harness.opaclient.model.OpaPolicySetEvaluationResponse;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.contracts.governance.PolicyMetadata;
import io.harness.pms.contracts.governance.PolicySetMetadata;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import io.harness.serializer.JsonUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OpaServiceImpl implements OpaService {
  private final OpaServiceClient opaServiceClient;

  public GovernanceMetadata evaluate(OpaEvaluationContext context, String accountId, String orgIdentifier,
      String projectIdentifier, String identifier, String action, String key) {
    OpaEvaluationResponseHolder response;
    try {
      String name = identifier;
      String userIdentifier = getUserIdentifier();
      String entityString = getEntityString(accountId, orgIdentifier, projectIdentifier, identifier);
      String entityMetadata = getEntityMetadataString(accountId, orgIdentifier, projectIdentifier, identifier, name);

      response = SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentials(key, accountId,
          orgIdentifier, projectIdentifier, action, entityString, entityMetadata, userIdentifier, context));
    } catch (Exception ex) {
      log.error("Exception while evaluating OPA rules", ex);
      throw new InvalidRequestException("Exception while evaluating OPA rules: " + ex.getMessage(), ex);
    }
    return mapResponseToMetadata(response);
  }

  private GovernanceMetadata mapResponseToMetadata(OpaEvaluationResponseHolder response) {
    return GovernanceMetadata.newBuilder()
        .setId(HarnessStringUtils.emptyIfNull(response.getId()))
        .setDeny(OpaConstants.OPA_STATUS_ERROR.equals(HarnessStringUtils.emptyIfNull(response.getStatus())))
        .setTimestamp(System.currentTimeMillis())
        .addAllDetails(mapPolicySetMetadata(response.getDetails()))
        .setStatus(HarnessStringUtils.emptyIfNull(response.getStatus()))
        .setAccountId(HarnessStringUtils.emptyIfNull(response.getAccount_id()))
        .setOrgId(HarnessStringUtils.emptyIfNull(response.getOrg_id()))
        .setProjectId(HarnessStringUtils.emptyIfNull(response.getProject_id()))
        .setEntity(HarnessStringUtils.emptyIfNull(response.getEntity()))
        .setType(HarnessStringUtils.emptyIfNull(response.getType()))
        .setAction(HarnessStringUtils.emptyIfNull(response.getAction()))
        .setCreated(response.getCreated())
        .build();
  }

  private List<PolicySetMetadata> mapPolicySetMetadata(List<OpaPolicySetEvaluationResponse> policySetResponse) {
    if (EmptyPredicate.isEmpty(policySetResponse)) {
      return Collections.emptyList();
    }
    List<PolicySetMetadata> policySetMetadataList = new ArrayList<>();
    for (OpaPolicySetEvaluationResponse setEvaluationResponse : policySetResponse) {
      policySetMetadataList.add(
          PolicySetMetadata.newBuilder()
              .setDeny(OpaConstants.OPA_STATUS_ERROR.equals(
                  HarnessStringUtils.emptyIfNull(setEvaluationResponse.getStatus())))
              .setStatus(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getStatus()))
              .setPolicySetName(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getName()))
              .addAllPolicyMetadata(mapPolicyMetadata(setEvaluationResponse.getDetails()))
              .setIdentifier(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getIdentifier()))
              .setCreated(setEvaluationResponse.getCreated())
              .setAccountId(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getAccount_id()))
              .setOrgId(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getOrg_id()))
              .setProjectId(HarnessStringUtils.emptyIfNull(setEvaluationResponse.getProject_id()))
              .build());
    }
    return policySetMetadataList;
  }

  private List<PolicyMetadata> mapPolicyMetadata(List<OpaPolicyEvaluationResponse> policyEvaluationResponses) {
    if (EmptyPredicate.isEmpty(policyEvaluationResponses)) {
      return Collections.emptyList();
    }
    List<PolicyMetadata> policyMetadataList = new ArrayList<>();
    for (OpaPolicyEvaluationResponse policyEvaluationResponse : policyEvaluationResponses) {
      policyMetadataList.add(
          PolicyMetadata.newBuilder()
              .setPolicyName(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getPolicy().getName()))
              .setIdentifier(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getPolicy().getIdentifier()))
              .setAccountId(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getPolicy().getAccount_id()))
              .setOrgId(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getPolicy().getOrg_id()))
              .setProjectId(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getPolicy().getProject_id()))
              .setCreated(policyEvaluationResponse.getPolicy().getCreated())
              .setUpdated(policyEvaluationResponse.getPolicy().getUpdated())
              .setStatus(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getStatus()))
              .setError(HarnessStringUtils.emptyIfNull(policyEvaluationResponse.getError()))
              .setSeverity(policyEvaluationResponse.getStatus())
              .addAllDenyMessages(policyEvaluationResponse.getDeny_messages())
              .build());
    }
    return policyMetadataList;
  }

  private String getEntityMetadataString(String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, String name) throws UnsupportedEncodingException {
    Map<String, String> metadataMap = new HashMap<>();

    metadataMap.put("accountIdentifier", accountId);
    metadataMap.put("identifier", identifier);
    metadataMap.put("name", name);
    if (orgIdentifier != null && !orgIdentifier.isEmpty()) {
      metadataMap.put("orgIdentifier", orgIdentifier);
    }
    if (projectIdentifier != null && !projectIdentifier.isEmpty()) {
      metadataMap.put("projectIdentifier", projectIdentifier);
    }
    return URLEncoder.encode(JsonUtils.asJson(metadataMap), StandardCharsets.UTF_8.toString());
  }

  private String getEntityString(String accountId, String orgIdentifier, String projectIdentifier, String identifier)
      throws UnsupportedEncodingException {
    String entityStringRaw = String.format("accountIdentifier:%s/orgIdentifier:%s/projectIdentifier:%s/identifier:%s",
        accountId, orgIdentifier, projectIdentifier, identifier);
    return URLEncoder.encode(entityStringRaw, StandardCharsets.UTF_8.toString());
  }

  public OpaEvaluationContext createEvaluationContext(String yaml, String key) throws IOException {
    return OpaEvaluationContext.builder()
        .entity(OpaUtils.extractObjectFromYamlString(yaml, key))
        .date(new Date())
        .build();
  }

  private String getUserIdentifier() {
    if (SourcePrincipalContextBuilder.getSourcePrincipal() == null
        || !USER.equals(SourcePrincipalContextBuilder.getSourcePrincipal().getType())) {
      return "";
    }
    UserPrincipal userPrincipal = (UserPrincipal) SourcePrincipalContextBuilder.getSourcePrincipal();
    return userPrincipal.getName();
  }
}
