package io.harness.delegate.beans.connector.awsconnector;


import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.GitConnectionNGCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.service.impl.aws.model.AwsCFRequest.AwsCFRequestType.GET_TEMPLATE_PARAMETERS;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@OwnedBy(HarnessTeam.CDP)
public class AwsCFTaskParamsRequest extends ConnectorTaskParams implements TaskParameters, ExecutionCapabilityDemander {
    @NotNull
    AwsConnectorDTO awsConnector;
    AwsTaskType awsTaskType;
    List<EncryptedDataDetail> encryptionDetails;
    String region;
    private String data;
    private GitStoreDelegateConfig gitStoreDelegateConfig;

    @Override
    public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
        List<ExecutionCapability> capabilityList = new ArrayList<>();
        capabilityList.add(GitConnectionNGCapability.builder()
                .gitConfig((GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO())
                .encryptedDataDetails(gitStoreDelegateConfig.getEncryptedDataDetails())
                .sshKeySpecDTO(gitStoreDelegateConfig.getSshKeySpecDTO())
                .build());

        GitConfigDTO gitConfigDTO = (GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO();
        if (isNotEmpty(gitConfigDTO.getDelegateSelectors())) {
            capabilityList.add(SelectorCapability.builder().selectors(gitConfigDTO.getDelegateSelectors()).build());
        }
        capabilityList.addAll(AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnector, maskingEvaluator));
        return capabilityList;
    }
}
