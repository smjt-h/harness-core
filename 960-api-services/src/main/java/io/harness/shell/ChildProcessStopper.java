/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.shell;

import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;

import io.harness.logging.CommandExecutionStatus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stop.ProcessStopper;

@Slf4j
public class ChildProcessStopper implements ProcessStopper {
  String fileName;
  File workingDirectory;
  Map<String, String> environment;

  public ChildProcessStopper(String fileName, File workingDirectory, Map<String, String> environment) {
    this.fileName = fileName;
    this.workingDirectory = workingDirectory;
    this.environment = environment;
  }

  public void stop(Process process) {
    ProcessExecutor processExecutor =
        new ProcessExecutor()
            .command("/bin/bash", "-c", "ps -ef | grep -m1 " + fileName + " | awk '{print $2}'")
            .directory(workingDirectory)
            .environment(environment)
            .readOutput(true);
    File scriptFile = new File(workingDirectory, "kill-" + fileName);
    try (FileOutputStream outputStream = new FileOutputStream(scriptFile)) {
      ProcessResult processResult = processExecutor.execute();
      CommandExecutionStatus commandExecutionStatus = processResult.getExitValue() == 0 ? SUCCESS : FAILURE;
      if (commandExecutionStatus == SUCCESS) {
        String ppid = processResult.getOutput().getUTF8();
        log.info("Pids: {}", ppid);
        String command = "list_descendants ()\n"
            + "{\n"
            + "  local children=$(ps -ef | grep $1 | awk '{print $2}')\n"
            + "\n"
            + "  for (( c=1; c<${#children[@]}-1 ; c++ ))\n"
            + "  do\n"
            + "    list_descendants ${children[c]}\n"
            + "  done\n"
            + "\n"
            + "  kill -9 ${children[0]}\n"
            + "}\n"
            + "\n"
            + "list_descendants " + ppid;
        outputStream.write(command.getBytes(Charset.forName("UTF-8")));
        processExecutor.command("/bin/bash", "kill-" + fileName);
        processExecutor.execute();
      }
    } catch (IOException | InterruptedException | TimeoutException e) {
      log.error("Exception in script execution ", e);
    }
    process.destroy();
  }
}
