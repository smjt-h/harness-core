package io.harness.pms.deserializer;

import io.harness.pms.contracts.execution.ExecutionErrorInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class ExecutionErrorInfoDeserializer extends JsonDeserializer<ExecutionErrorInfo> {
  @Override
  public ExecutionErrorInfo deserialize(JsonParser p, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    ExecutionErrorInfo.Builder builder = ExecutionErrorInfo.newBuilder();
    String p1 = p.readValueAsTree().toString();
    JsonFormat.parser().ignoringUnknownFields().merge(p1, builder);
    ExecutionErrorInfo executionErrorInfo = builder.build();
    return executionErrorInfo;
  }
}
