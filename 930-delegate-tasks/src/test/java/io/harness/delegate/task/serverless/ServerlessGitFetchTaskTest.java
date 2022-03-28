/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.NGDelegateLogCallback;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.serverless.*;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.request.ServerlessDeployRequest;
import io.harness.delegate.task.serverless.request.ServerlessGitFetchRequest;
import io.harness.delegate.task.serverless.response.ServerlessGitFetchResponse;
import io.harness.git.model.FetchFilesResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.ServerlessCommandUnitConstants;
import io.harness.serverless.model.ServerlessAwsLambdaConfig;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import lombok.SneakyThrows;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.BRETT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

@OwnedBy(CDP)
public class ServerlessGitFetchTaskTest extends CategoryTest {

    @Mock GitDecryptionHelper gitDecryptionHelper;
    @Mock ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
    @InjectMocks ServerlessGitFetchTask serverlessGitFetchTask;

    String identifier = "iden";
    String manifestType = "serverless-lambda";
    String url = "url";
    ScmConnector scmConnector = GitConfigDTO.builder().url(url).build();
    String branch = "bran";
    String path = "path/";
    String accountId = "account";
    String configOverridePath = "override/";
    GitStoreDelegateConfig gitStoreDelegateConfig = GitStoreDelegateConfig.builder().gitConfigDTO(scmConnector).
            fetchType(FetchType.BRANCH).branch(branch).optimizedFilesFetch(true).path(path).build();
    ServerlessGitFetchFileConfig serverlessGitFetchFileConfig = ServerlessGitFetchFileConfig.builder().identifier(identifier)
            .manifestType(manifestType).configOverridePath(configOverridePath).build();
    String activityId = "activityid";
    TaskParameters taskParameters = ServerlessGitFetchRequest.builder().activityId(activityId).accountId(accountId).
            serverlessGitFetchFileConfig(serverlessGitFetchFileConfig).shouldOpenLogStream(true).closeLogStream(true).build();

    @Test
    @Owner(developers = BRETT)
    @Category(UnitTests.class)
    public void runTest() {

        String combinedPath = path+configOverridePath;
        List<String> filePaths = Collections.singletonList(combinedPath);
        FetchFilesResult fetchFilesResult = FetchFilesResult.builder().build();
        doReturn(fetchFilesResult).when(serverlessGitFetchTaskHelper).fetchFileFromRepo(gitStoreDelegateConfig, filePaths,
                accountId, (GitConfigDTO) scmConnector);
        DelegateResponseData delegateResponseData = ServerlessGitFetchResponse.builder().build();
        assertThat(serverlessGitFetchTask.run(taskParameters)).isEqualTo(fetchFilesResult);
    }
}
