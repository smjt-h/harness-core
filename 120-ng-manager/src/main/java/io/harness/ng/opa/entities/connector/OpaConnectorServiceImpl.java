package io.harness.ng.opa.entities.connector;

import com.google.inject.Inject;
import io.harness.beans.FeatureName;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.opa.OpaEvaluationContext;
import io.harness.ng.opa.OpaService;
import io.harness.opaclient.model.OpaConstants;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OpaConnectorServiceImpl implements OpaConnectorService{
    private OpaService opaService;
    private FeatureFlagService featureFlagService;


    public void evaluatePolicies(String accountId, String expandedJson, String orgIdentifier,
                                 String projectIdentifier, String action, String planExecutionId){
        if(!featureFlagService.isEnabled(FeatureName.OPA_CONNECTOR_GOVERNANCE, accountId)){
            throw new InvalidRequestException("This feature is not allowed for this account");
        }
        OpaEvaluationContext context;
        try {
            context = opaService.createEvaluationContext(expandedJson, OpaConstants.OPA_EVALUATION_TYPE_CONNECTOR);
        }
        catch (IOException ex){
            log.error("Could not create OPA evaluation context", ex);
        }

    }
}
