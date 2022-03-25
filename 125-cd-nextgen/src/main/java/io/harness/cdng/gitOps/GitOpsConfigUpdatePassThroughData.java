package io.harness.cdng.gitOps;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfig;
import io.harness.pms.sdk.core.steps.io.PassThroughData;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

import java.util.List;
import java.util.Map;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@OwnedBy(CDP)
@Value
@Builder
@TypeAlias("gitOpsConfigUpdatePassThroughData")
@RecasterAlias("io.harness.cdng.gitOps.GitOpsConfigUpdatePassThroughData")
public class GitOpsConfigUpdatePassThroughData implements PassThroughData {
    Map<String, String> stringMap;
    @Setter List<String> filePaths;
    StoreConfig store;
}
