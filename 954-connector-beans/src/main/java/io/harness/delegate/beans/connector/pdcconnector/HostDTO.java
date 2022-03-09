package io.harness.delegate.beans.connector.pdcconnector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.annotations.dev.OwnedBy;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("HostDTO")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "HostDTO", description = "This entity contains the Host details")
public class HostDTO {
  @JsonProperty("hostname") String hostName;
  @JsonProperty("hostAttributes") List<HostAttributeDTO> hostAttributes;
}
