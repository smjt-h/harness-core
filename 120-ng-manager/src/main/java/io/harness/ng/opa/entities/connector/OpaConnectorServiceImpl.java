package io.harness.ng.opa.entities.connector;

import io.harness.beans.FeatureName;
import io.harness.connector.ConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.ng.opa.OpaEvaluationContext;
import io.harness.ng.opa.OpaService;
import io.harness.opaclient.model.OpaConstants;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.inject.Inject;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class OpaConnectorServiceImpl implements OpaConnectorService {
  private OpaService opaService;
  private FeatureFlagService featureFlagService;

  public void evaluatePoliciesWithEntity(String accountId, ConnectorDTO connectorDTO, String orgIdentifier, String projectIdentifier,
                                         String action, String identifier){
    if (!featureFlagService.isEnabled(FeatureName.OPA_CONNECTOR_GOVERNANCE, accountId)) {
      throw new InvalidRequestException("This feature is not allowed for this account");
    }
    OpaEvaluationContext context;

    try {
      String expandedJson = getConnectorYaml(connectorDTO);
      context = opaService.createEvaluationContext(expandedJson, OpaConstants.OPA_EVALUATION_TYPE_CONNECTOR);
      opaService.evaluate(context, accountId, orgIdentifier, projectIdentifier, identifier, action,
              OpaConstants.OPA_EVALUATION_TYPE_CONNECTOR);
    } catch (IOException ex) {
      log.error("Could not create OPA evaluation context", ex);
    }
  }

  private String getConnectorYaml(ConnectorDTO connectorDTO){
    String connectorYaml = null;
    ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID));
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    try {
      connectorYaml = objectMapper.writeValueAsString(connectorDTO);
    } catch (Exception ex) {
      log.error("Failed while converting to connector yaml format", ex);
    }
    return connectorYaml;
  }

  public void evaluatePoliciesWithJson(String accountId, String expandedJson, String orgIdentifier, String projectIdentifier,
      String action, String identifier) {
    if (!featureFlagService.isEnabled(FeatureName.OPA_CONNECTOR_GOVERNANCE, accountId)) {
      throw new InvalidRequestException("This feature is not allowed for this account");
    }
    OpaEvaluationContext context;
    try {
      context = opaService.createEvaluationContext(expandedJson, OpaConstants.OPA_EVALUATION_TYPE_CONNECTOR);
      opaService.evaluate(context, accountId, orgIdentifier, projectIdentifier, identifier, action,
          OpaConstants.OPA_EVALUATION_TYPE_CONNECTOR);
    } catch (IOException ex) {
      log.error("Could not create OPA evaluation context", ex);
    }
  }
}
