package io.harness.handlers;

import io.harness.pms.contracts.plan.ExecutionTriggerInfo;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class TriggerInfoDeserializer extends JsonDeserializer<ExecutionTriggerInfo> {
  @Override
  public ExecutionTriggerInfo deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
    ExecutionTriggerInfo.Builder builder = ExecutionTriggerInfo.newBuilder();
    String p1 = p.readValueAsTree().toString();
    JsonFormat.parser().ignoringUnknownFields().merge(p1, builder);
    ExecutionTriggerInfo triggerInfo = builder.build();
    return triggerInfo;
  }
}
