/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.logstreaming.UnitProgressDataMapper;
import io.harness.delegate.exception.TaskNGDataException;
import io.harness.delegate.serverless.ServerlessCommandTaskHandler;
import io.harness.delegate.task.serverless.request.ServerlessCommandRequest;
import io.harness.delegate.task.serverless.response.ServerlessCommandResponse;
import io.harness.serverless.model.ServerlessDelegateTaskParams;

import software.wings.delegatetasks.ExceptionMessageSanitizer;

import com.google.inject.Inject;
import java.nio.file.Paths;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class ServerlessDelegateTaskHelper {
  @Inject private Map<String, ServerlessCommandTaskHandler> commandTaskTypeToTaskHandlerMap;
  @Inject private ServerlessInfraConfigHelper serverlessInfraConfigHelper;

  private static final String WORKING_DIR_BASE = "./repository/serverless/";

  public ServerlessCommandResponse getServerlessCommandResponse(
      ServerlessCommandRequest serverlessCommandRequest, ILogStreamingTaskClient iLogStreamingTaskClient) {
    CommandUnitsProgress commandUnitsProgress = serverlessCommandRequest.getCommandUnitsProgress() != null
        ? serverlessCommandRequest.getCommandUnitsProgress()
        : CommandUnitsProgress.builder().build();

    log.info("Starting task execution for command: {}", serverlessCommandRequest.getServerlessCommandType().name());
    decryptRequestDTOs(serverlessCommandRequest);
    String workingDirectory = Paths.get(WORKING_DIR_BASE, convertBase64UuidToCanonicalForm(generateUuid()))
                                  .normalize()
                                  .toAbsolutePath()
                                  .toString();
    ServerlessCommandTaskHandler commandTaskHandler =
        commandTaskTypeToTaskHandlerMap.get(serverlessCommandRequest.getServerlessCommandType().name());
    try {
      createDirectoryIfDoesNotExist(workingDirectory);
      waitForDirectoryToBeAccessibleOutOfProcess(workingDirectory, 10);
      ServerlessDelegateTaskParams serverlessDelegateTaskParams =
          ServerlessDelegateTaskParams.builder().workingDirectory(workingDirectory).build();
      ServerlessCommandResponse serverlessCommandResponse = commandTaskHandler.executeTask(
          serverlessCommandRequest, serverlessDelegateTaskParams, iLogStreamingTaskClient, commandUnitsProgress);
      serverlessCommandResponse.setCommandUnitsProgress(
          UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress));
      return serverlessCommandResponse;
    } catch (Exception e) {
      Exception sanitizedException = ExceptionMessageSanitizer.sanitizeException(e);
      log.error("Exception in processing serverless task [{}]",
          serverlessCommandRequest.getCommandName() + ":" + serverlessCommandRequest.getServerlessCommandType(),
          sanitizedException);
      throw new TaskNGDataException(
          UnitProgressDataMapper.toUnitProgressData(commandUnitsProgress), sanitizedException);
    } finally {
      cleanup(workingDirectory);
    }
  }

  private void decryptRequestDTOs(ServerlessCommandRequest serverlessCommandRequest) {
    serverlessInfraConfigHelper.decryptServerlessInfraConfig(serverlessCommandRequest.getServerlessInfraConfig());
  }

  private void cleanup(String workingDirectory) {
    try {
      log.warn("Cleaning up directory " + workingDirectory);
      deleteDirectoryAndItsContentIfExists(workingDirectory);
    } catch (Exception ex) {
      log.warn("Exception in directory cleanup.", ExceptionMessageSanitizer.sanitizeException(ex));
    }
  }
}
