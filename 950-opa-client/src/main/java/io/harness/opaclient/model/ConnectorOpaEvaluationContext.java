package io.harness.opaclient.model;


import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
@OwnedBy(HarnessTeam.PL)
public class ConnectorOpaEvaluationContext {
    Object connector;
    UserOpaEvaluationContext user;
    String action;
    Date date;
}
