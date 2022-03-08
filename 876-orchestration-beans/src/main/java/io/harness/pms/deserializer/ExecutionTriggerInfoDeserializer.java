package io.harness.pms.deserializer;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class ExecutionTriggerInfoDeserializer extends JsonDeserializer<ExecutionTriggerInfo> {
  @Override
  public ExecutionTriggerInfo deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    ExecutionTriggerInfo.Builder builder = ExecutionTriggerInfo.newBuilder();
    String p1 = p.readValueAsTree().toString();
    JsonFormat.parser().ignoringUnknownFields().merge(p1, builder);
    return builder.build();
  }
}
