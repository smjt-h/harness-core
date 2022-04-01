/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.serverless.ServerlessAwsLambdaManifestSchema;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.serverless.ServerlessCliResponse;
import io.harness.serverless.ServerlessClient;
import io.harness.serverless.model.ServerlessDelegateTaskParams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import java.util.Arrays;
import java.util.Optional;

import static io.harness.rule.OwnerRule.*;
import static org.assertj.core.api.Assertions.assertThat;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static org.mockito.Mockito.doReturn;

@OwnedBy(CDP)
public class ServerlessAwsCommandTaskHelperTest extends CategoryTest {

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock ServerlessTaskPluginHelper serverlessTaskPluginHelper;

    @InjectMocks ServerlessAwsCommandTaskHelper serverlessAwsCommandTaskHelper;
    ServerlessAwsLambdaManifestSchema serverlessAwsLambdaManifestSchema = ServerlessAwsLambdaManifestSchema.builder()
            .plugins(Arrays.asList("asfd", "asfdasdf")).build();
    ServerlessDelegateTaskParams serverlessDelegateTaskParams = ServerlessDelegateTaskParams.builder().build();
    @Mock LogCallback logCallback;
    @Mock ServerlessClient serverlessClient;

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void testGetPreviousVersionTimeStamp() {
        String output = "Warning: Invalid configuration encountered\n" +
                "  at 'provider.tracing': must be object\n" +
                "\n" +
                "Learn more about configuration validation here: http://slss.io/configuration-validation\n" +

                "2022-03-11 08:48:51 UTC\n" +
                "Timestamp: 1646988531400\n" +
                "Files:\n" +
                "  - aws-node-http-api-project-1.zip\n" +
                "  - compiled-cloudformation-template.json\n" +
                "  - custom-resources.zip\n" +
                "  - serverless-state.json\n" +
                "2022-03-11 08:58:16 UTC\n" +
                "Timestamp: 1646989096845\n" +
                "Files:\n" +
                "  - aws-node-http-api-project-1.zip\n" +
                "  - compiled-cloudformation-template.json\n" +
                "  - custom-resources.zip\n" +
                "  - serverless-state.json";
        assertThat(serverlessAwsCommandTaskHelper.getPreviousVersionTimeStamp(output)).isEqualTo(Optional.of("1646989096845"));
    }

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void installPluginsSuccessTest() throws Exception{

        ServerlessCliResponse serverlessCliResponse = ServerlessCliResponse.builder().commandExecutionStatus(
                CommandExecutionStatus.SUCCESS).build();
        doReturn(serverlessCliResponse).when(serverlessTaskPluginHelper).installServerlessPlugin(serverlessDelegateTaskParams,
                serverlessClient, "asfd", logCallback, 10);
        doReturn(serverlessCliResponse).when(serverlessTaskPluginHelper).installServerlessPlugin(serverlessDelegateTaskParams,
                serverlessClient, "asfdasdf", logCallback, 10);
        assertThat(serverlessAwsCommandTaskHelper.installPlugins(serverlessAwsLambdaManifestSchema, serverlessDelegateTaskParams,
                logCallback, serverlessClient, 10)).isTrue();
    }

}
