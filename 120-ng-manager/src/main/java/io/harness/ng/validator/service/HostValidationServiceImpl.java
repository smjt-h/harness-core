package io.harness.ng.validator.service;

import static io.harness.connector.HostValidationResult.HostValidationStatus.FAILED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;
import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.DEFAULT_HOST_VALIDATION_FAILED_MSG;
import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.TRUE_STR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.DelegateTaskRequest;
import io.harness.connector.HostValidationResult;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.task.utils.PhysicalDataCenterConstants;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.manage.ManagedExecutorService;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.validator.HostValidatoionService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class HostValidationServiceImpl implements HostValidatoionService {
  @Inject private SshKeySpecDTOHelper sshKeySpecDTOHelper;
  @Inject private NGSecretServiceV2 ngSecretServiceV2;
  @Inject private TaskSetupAbstractionHelper taskSetupAbstractionHelper;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final Executor executor = new ManagedExecutorService(Executors.newFixedThreadPool(4));

  @Override
  public List<HostValidationResult> validateSSHHosts(List<String> hosts, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String secretIdentifier) {
    if (hosts.isEmpty()) {
      return Collections.emptyList();
    }

    CompletableFutures<HostValidationResult> validateHostTasks = new CompletableFutures<>(executor);
    for (String host : hosts) {
      validateHostTasks.supplyAsyncExceptionally(
          ()
              -> validateSSHHost(host, accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier),
          ex -> HostValidationResult.builder().status(FAILED).error(buildErrorDetailsWithMsg(ex.getMessage())).build());
    }

    CompletableFuture<List<HostValidationResult>> listCompletableFuture = validateHostTasks.allOf();
    try {
      return new ArrayList<>(listCompletableFuture.get());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), USER);
    } catch (ExecutionException ex) {
      throw new InvalidRequestException(ex.getMessage(), USER);
    }
  }

  @Override
  public HostValidationResult validateSSHHost(@NotNull String host, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String secretIdentifier) {
    Optional<Secret> secretOptional =
        ngSecretServiceV2.get(accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifier);
    if (!secretOptional.isPresent()) {
      throw new InvalidArgumentsException(
          String.format("No found secret for host validation, secret identifier: %s", secretIdentifier), USER_SRE);
    }
    if (SecretType.SSHKey != secretOptional.get().getType()) {
      throw new InvalidArgumentsException(
          String.format("Secret is not SSH type, secret identifier: %s", secretIdentifier), USER_SRE);
    }
    if (isBlank(host)) {
      throw new InvalidArgumentsException("SSH host cannot be empty", USER_SRE);
    }

    SSHKeySpecDTO secretSpecDTO = (SSHKeySpecDTO) secretOptional.get().getSecretSpec().toDTO();
    BaseNGAccess baseNGAccess = getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails =
        sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(secretSpecDTO, baseNGAccess);

    DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountIdentifier)
            .taskType(TaskType.NG_SSH_VALIDATION.name())
            .taskParameters(SSHTaskParams.builder()
                                .host(host)
                                .encryptionDetails(encryptionDetails)
                                .sshKeySpec(secretSpecDTO)
                                .build())
            .taskSetupAbstractions(setupTaskAbstractions(accountIdentifier, orgIdentifier, projectIdentifier))
            .executionTimeout(Duration.ofSeconds(PhysicalDataCenterConstants.EXECUTION_TIMEOUT_IN_SECONDS))
            .build();

    DelegateResponseData delegateResponseData = this.delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (delegateResponseData instanceof SSHConfigValidationTaskResponse) {
      SSHConfigValidationTaskResponse responseData = (SSHConfigValidationTaskResponse) delegateResponseData;
      return HostValidationResult.builder()
          .status(HostValidationResult.HostValidationStatus.fromBoolean(responseData.isConnectionSuccessful()))
          .error(buildErrorDetailsWithMsg(responseData.getErrorMessage()))
          .build();
    }

    return HostValidationResult.builder()
        .status(FAILED)
        .error(buildErrorDetailsWithMsg(getErrorMessageFromDelegateResponseData(delegateResponseData)))
        .build();
  }

  private BaseNGAccess getBaseNGAccess(
      final String accountIdentifier, final String orgIdentifier, final String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private Map<String, String> setupTaskAbstractions(
      final String accountIdIdentifier, final String orgIdentifier, final String projectIdentifier) {
    Map<String, String> abstractions = new HashMap<>(2);
    final String owner = taskSetupAbstractionHelper.getOwner(accountIdIdentifier, orgIdentifier, projectIdentifier);
    if (isNotEmpty(owner)) {
      abstractions.put(OWNER, owner);
    }
    abstractions.put(NG, TRUE_STR);
    return abstractions;
  }

  private String getErrorMessageFromDelegateResponseData(DelegateResponseData delegateResponseData) {
    return (delegateResponseData instanceof RemoteMethodReturnValueData)
        ? ((RemoteMethodReturnValueData) delegateResponseData).getException().getMessage()
        : DEFAULT_HOST_VALIDATION_FAILED_MSG;
  }

  private ErrorDetail buildErrorDetailsWithMsg(final String message) {
    return ErrorDetail.builder().message(message).build();
  }
}
