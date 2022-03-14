package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionRequestMetadata;
import io.harness.pms.contracts.governance.ExpansionRequestProto;
import io.harness.pms.yaml.YamlUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class ExpansionRequestBatchHelper {
  public ExpansionRequestData getBatchesByModule(
      Set<ExpansionRequest> expansionRequests, ExpansionRequestMetadata expansionRequestMetadata) {
    Set<ModuleType> modulesInRequests =
        expansionRequests.stream().map(ExpansionRequest::getModule).collect(Collectors.toSet());

    // separate all requests by module so that each module's requests are handled independently
    Map<ModuleType, Set<ExpansionRequest>> requestsPerModule = new HashMap<>();
    for (ModuleType module : modulesInRequests) {
      Set<ExpansionRequest> currModuleRequests =
          expansionRequests.stream()
              .filter(expansionRequest -> expansionRequest.getModule().equals(module))
              .collect(Collectors.toSet());
      requestsPerModule.put(module, currModuleRequests);
    }

    // get request batch for each module, and build fqn to requestUUID map
    Map<ModuleType, ExpansionRequestBatch> moduleToRequestBatch = new HashMap<>();
    Map<String, Set<String>> uuidToFqnSet = new HashMap<>();
    for (ModuleType module : requestsPerModule.keySet()) {
      Set<ExpansionRequest> currModuleExpansionRequests = requestsPerModule.get(module);
      RequestBatchWithUuidMap requestBatchWithUuidMap =
          buildRequestBatchForModule(currModuleExpansionRequests, expansionRequestMetadata);
      moduleToRequestBatch.put(module, requestBatchWithUuidMap.getBatch());
      uuidToFqnSet.putAll(requestBatchWithUuidMap.getUuidToFqnSet());
    }
    return ExpansionRequestData.builder().moduleToRequestBatch(moduleToRequestBatch).uuidToFqnSet(uuidToFqnSet).build();
  }

  /**
   * @param expansionRequests: all requests should have the same module
   */
  RequestBatchWithUuidMap buildRequestBatchForModule(
      Set<ExpansionRequest> expansionRequests, ExpansionRequestMetadata expansionRequestMetadata) {
    // separate all requests by key so that each key's requests are handled independently
    Map<String, Set<ExpansionRequest>> requestsPerKey = new HashMap<>();
    for (ExpansionRequest expansionRequest : expansionRequests) {
      String expansionRequestKey = expansionRequest.getKey();
      if (requestsPerKey.containsKey(expansionRequestKey)) {
        requestsPerKey.get(expansionRequestKey).add(expansionRequest);
      } else {
        Set<ExpansionRequest> currKeyRequests = new HashSet<>();
        currKeyRequests.add(expansionRequest);
        requestsPerKey.put(expansionRequestKey, currKeyRequests);
      }
    }

    // get request batch and fqn to requestUUID map for each key, and build full fqn to requestUUID map for the module
    ExpansionRequestBatch.Builder batchBuilder =
        ExpansionRequestBatch.newBuilder().setRequestMetadata(expansionRequestMetadata);
    Map<String, Set<String>> uuidToFqnSet = new HashMap<>();
    for (String expansionRequestKey : requestsPerKey.keySet()) {
      Set<ExpansionRequest> currKeyRequests = requestsPerKey.get(expansionRequestKey);
      RequestBatchWithUuidMap requestBatchWithUuidMap =
          buildRequestBatchForModuleAndKey(currKeyRequests, expansionRequestKey, expansionRequestMetadata);
      batchBuilder.addAllExpansionRequestProto(requestBatchWithUuidMap.getBatch().getExpansionRequestProtoList());
      uuidToFqnSet.putAll(requestBatchWithUuidMap.getUuidToFqnSet());
    }
    ExpansionRequestBatch expansionRequestBatch = batchBuilder.build();
    return RequestBatchWithUuidMap.builder().batch(expansionRequestBatch).uuidToFqnSet(uuidToFqnSet).build();
  }

  /**
   * @param expansionRequests: all requests should have the same module and same key
   */
  RequestBatchWithUuidMap buildRequestBatchForModuleAndKey(
      Set<ExpansionRequest> expansionRequests, String key, ExpansionRequestMetadata expansionRequestMetadata) {
    // separate all requests by value so that each value's requests are mapped to one common request
    Map<JsonNode, Set<ExpansionRequest>> requestsPerValue = new HashMap<>();
    for (ExpansionRequest expansionRequest : expansionRequests) {
      JsonNode value = expansionRequest.getFieldValue();
      if (requestsPerValue.containsKey(value)) {
        requestsPerValue.get(value).add(expansionRequest);
      } else {
        Set<ExpansionRequest> currKeyRequests = new HashSet<>();
        currKeyRequests.add(expansionRequest);
        requestsPerValue.put(value, currKeyRequests);
      }
    }

    // build common request for each value, and create the fqnToUUIDMap
    ExpansionRequestBatch.Builder batchBuilder =
        ExpansionRequestBatch.newBuilder().setRequestMetadata(expansionRequestMetadata);
    Map<String, Set<String>> uuidToFqnSet = new HashMap<>();
    for (JsonNode value : requestsPerValue.keySet()) {
      String requestUuid = generateUuid();
      ExpansionRequestProto expansionRequestProto = ExpansionRequestProto.newBuilder()
                                                        .setUuid(requestUuid)
                                                        .setKey(key)
                                                        .setValue(convertToByteString(value))
                                                        .build();
      batchBuilder.addExpansionRequestProto(expansionRequestProto);
      Set<String> fqnSet =
          requestsPerValue.get(value).stream().map(ExpansionRequest::getFqn).collect(Collectors.toSet());
      uuidToFqnSet.put(requestUuid, fqnSet);
    }
    ExpansionRequestBatch expansionRequestBatch = batchBuilder.build();
    return RequestBatchWithUuidMap.builder().batch(expansionRequestBatch).uuidToFqnSet(uuidToFqnSet).build();
  }

  ByteString convertToByteString(JsonNode fieldValue) {
    String s = YamlUtils.write(fieldValue);
    return ByteString.copyFromUtf8(s);
  }

  @Value
  @Builder
  class RequestBatchWithUuidMap {
    ExpansionRequestBatch batch;
    Map<String, Set<String>> uuidToFqnSet;
  }
}
