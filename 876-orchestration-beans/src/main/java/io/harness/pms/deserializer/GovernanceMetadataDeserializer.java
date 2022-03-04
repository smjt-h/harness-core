package io.harness.pms.deserializer;

import io.harness.pms.contracts.governance.GovernanceMetadata;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;

public class GovernanceMetadataDeserializer extends JsonDeserializer<GovernanceMetadata> {
  @Override
  public GovernanceMetadata deserialize(JsonParser p, DeserializationContext deserializationContext)
      throws IOException, JsonProcessingException {
    GovernanceMetadata.Builder builder = GovernanceMetadata.newBuilder();
    String p1 = p.readValueAsTree().toString();
    JsonFormat.parser().ignoringUnknownFields().merge(p1, builder);
    GovernanceMetadata governanceMetadata = builder.build();
    return governanceMetadata;
  }
}
