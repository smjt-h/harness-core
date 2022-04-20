package io.harness.ng.opa;

import com.google.inject.Inject;
import io.harness.network.SafeHttpCall;
import io.harness.opaclient.OpaServiceClient;
import io.harness.opaclient.OpaUtils;
import io.harness.opaclient.model.OpaConstants;
import io.harness.opaclient.model.OpaEvaluationResponseHolder;
import io.harness.opaclient.model.PipelineOpaEvaluationContext;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.UserPrincipal;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Date;

import static io.harness.security.dto.PrincipalType.USER;

@AllArgsConstructor(onConstructor = @__({ @Inject}))
@Slf4j
public class OpaServiceImpl implements  OpaService{
    private final OpaServiceClient opaServiceClient;

    public void evaluate(){
        OpaEvaluationResponseHolder response;
        try{
            String userIdentifier = getUserIdentifier();

            response = SafeHttpCall.executeWithExceptions(opaServiceClient.evaluateWithCredentials(key, accountId,
                    orgIdentifier, projectIdentifier, action, entityString, entityMetadata, userIdentifier, context))
        }
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
