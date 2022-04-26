package io.harness.ng.opa;

import static io.harness.security.dto.PrincipalType.USER;

import io.harness.exception.InvalidRequestException;
import io.harness.network.SafeHttpCall;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.OpaUtils;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import io.harness.serializer.JsonUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OpaServiceImpl implements OpaService {
  private final OpaServiceClient opaServiceClient;

  public void evaluate(OpaEvaluationContext context, String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, String action, String key) {
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
  }

  private String getEntityMetadataString(String accountId, String orgIdentifier, String projectIdentifier,
      String identifier, String name) throws UnsupportedEncodingException {
    Map<String, String> metadataMap = ImmutableMap.<String, String>builder()
                                          .put("accountIdentifier", accountId)
                                          .put("orgIdentifier", orgIdentifier)
                                          .put("projectIdentifier", projectIdentifier)
                                          .put("identifier", identifier)
                                          .put("name", name)
                                          .build();
    return URLEncoder.encode(JsonUtils.asJson(metadataMap), StandardCharsets.UTF_8.toString());
  }

  private String getEntityString(String accountId, String orgIdentifier, String projectIdentifier, String identifier)
      throws UnsupportedEncodingException {
    String entityStringRaw =
        String.format("accountIdentifier:%s/orgIdentifier:%s/projectIdentifier:%s/pipelineIdentifier:%s", accountId,
            orgIdentifier, projectIdentifier, identifier);
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
