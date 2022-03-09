package io.harness.delegate.beans.connector.pdcconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.connector.DelegateSelectable;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "PhysicalDataCenterConnectorDTO", description = "This contains Physical Data Center connector details")

public class PhysicalDataCenterConnectorDTO extends ConnectorConfigDTO implements DelegateSelectable {
  @JsonDeserialize(using = HostDTOsDeserializer.class) @JsonProperty("hosts") @Valid List<HostDTO> hosts;

  @NotNull @SecretReference @ApiModelProperty(dataType = "string") SecretRefData sshKeyRef;
  Set<String> delegateSelectors;

  @Override
  public List<DecryptableEntity> getDecryptableEntities() {
    return Collections.singletonList(this);
  }
}
