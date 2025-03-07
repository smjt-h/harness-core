/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.pms.pipeline.service.PMSPipelineServiceStepHelper.LIBRARY;
import static io.harness.telemetry.Destination.AMPLITUDE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eventsframework.api.EventsFrameworkDownException;
import io.harness.eventsframework.schemas.entity.EntityDetailProtoDTO;
import io.harness.eventsframework.schemas.entity.IdentifierRefProtoDTO;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.InvalidYamlException;
import io.harness.exception.ScmException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.utils.GitEntityFilePath;
import io.harness.gitsync.common.utils.GitSyncFilePathUtils;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.scm.EntityObjectIdUtils;
import io.harness.grpc.utils.StringValueUtils;
import io.harness.pms.contracts.steps.StepInfo;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.instrumentaion.PipelineInstrumentationConstants;
import io.harness.pms.pipeline.CommonStepInfo;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineEntityUtils;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.pms.pipeline.PipelineMetadataV2;
import io.harness.pms.pipeline.StepCategory;
import io.harness.pms.pipeline.StepPalleteFilterWrapper;
import io.harness.pms.pipeline.StepPalleteInfo;
import io.harness.pms.pipeline.StepPalleteModuleInfo;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.variables.VariableCreatorMergeService;
import io.harness.pms.yaml.YamlUtils;
import io.harness.repositories.pipeline.PMSPipelineRepository;
import io.harness.telemetry.TelemetryReporter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceImpl implements PMSPipelineService {
  @Inject private PMSPipelineRepository pmsPipelineRepository;
  @Inject private PmsSdkInstanceService pmsSdkInstanceService;
  @Inject private VariableCreatorMergeService variableCreatorMergeService;
  @Inject private PMSPipelineServiceHelper pmsPipelineServiceHelper;
  @Inject private PMSPipelineServiceStepHelper pmsPipelineServiceStepHelper;
  @Inject private GitSyncSdkService gitSyncSdkService;
  @Inject private CommonStepInfo commonStepInfo;
  @Inject private TelemetryReporter telemetryReporter;
  @Inject private PipelineMetadataService pipelineMetadataService;
  public static String PIPELINE_SAVE = "pipeline_save";
  public static String PIPELINE_SAVE_ACTION_TYPE = "action";
  public static String CREATING_PIPELINE = "creating new pipeline";
  public static String UPDATING_PIPELINE = "updating existing pipeline";
  public static String PIPELINE_NAME = "pipelineName";
  public static String ACCOUNT_ID = "accountId";
  public static String ORG_ID = "orgId";
  public static String PROJECT_ID = "projectId";

  private static final String DUP_KEY_EXP_FORMAT_STRING =
      "Pipeline [%s] under Project[%s], Organization [%s] already exists";

  @Override
  public PipelineEntity create(PipelineEntity pipelineEntity) {
    try {
      PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(),
          pipelineEntity.getIdentifier());

      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);
      PipelineEntity createdEntity = pmsPipelineRepository.save(entityWithUpdatedInfo);
      sendPipelineSaveTelemetryEvent(createdEntity, CREATING_PIPELINE);
      return createdEntity;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format(DUP_KEY_EXP_FORMAT_STRING, pipelineEntity.getIdentifier(),
                                            pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);

    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);

    } catch (ExplanationException | ScmException e) {
      log.error("Error while updating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while saving pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while saving pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public Optional<PipelineEntity> get(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier, boolean deleted) {
    try {
      return pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
          accountId, orgIdentifier, projectIdentifier, identifier, !deleted);
    } catch (Exception e) {
      log.error(String.format("Error while retrieving pipeline [%s]", identifier), e);
      throw new InvalidRequestException(
          String.format("Error while retrieving pipeline [%s]: %s", identifier, ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineEntity updatePipelineYaml(PipelineEntity pipelineEntity, ChangeType changeType) {
    PMSPipelineServiceHelper.validatePresenceOfRequiredFields(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier());

    if (GitContextHelper.getGitEntityInfo() != null && GitContextHelper.getGitEntityInfo().isNewBranch()) {
      // sending old entity as null here because a new mongo entity will be created. If audit trail needs to be added
      // to git synced projects, a get call needs to be added here to the base branch of this pipeline update
      return makePipelineUpdateCall(pipelineEntity, null, changeType);
    }
    Optional<PipelineEntity> optionalOriginalEntity =
        pmsPipelineRepository.findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
            pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(),
            pipelineEntity.getIdentifier(), true);
    if (!optionalOriginalEntity.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier()));
    }
    PipelineEntity entityToUpdate = optionalOriginalEntity.get();
    PipelineEntity tempEntity = entityToUpdate.withYaml(pipelineEntity.getYaml())
                                    .withName(pipelineEntity.getName())
                                    .withDescription(pipelineEntity.getDescription())
                                    .withTags(pipelineEntity.getTags())
                                    .withIsEntityInvalid(false)
                                    .withTemplateReference(pipelineEntity.getTemplateReference())
                                    .withAllowStageExecutions(pipelineEntity.getAllowStageExecutions());

    return makePipelineUpdateCall(tempEntity, entityToUpdate, changeType);
  }

  @Override
  public PipelineEntity syncPipelineEntityWithGit(EntityDetailProtoDTO entityDetail) {
    IdentifierRefProtoDTO identifierRef = entityDetail.getIdentifierRef();
    String accountId = StringValueUtils.getStringFromStringValue(identifierRef.getAccountIdentifier());
    String orgId = StringValueUtils.getStringFromStringValue(identifierRef.getOrgIdentifier());
    String projectId = StringValueUtils.getStringFromStringValue(identifierRef.getProjectIdentifier());
    String pipelineId = StringValueUtils.getStringFromStringValue(identifierRef.getIdentifier());

    Optional<PipelineEntity> optionalPipelineEntity;
    try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(null, false)) {
      optionalPipelineEntity = get(accountId, orgId, projectId, pipelineId, false);
    }
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(
          PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(orgId, projectId, pipelineId));
    }
    return makePipelineUpdateCall(optionalPipelineEntity.get(), optionalPipelineEntity.get(), ChangeType.ADD);
  }

  private PipelineEntity makePipelineUpdateCall(
      PipelineEntity pipelineEntity, PipelineEntity oldEntity, ChangeType changeType) {
    try {
      PipelineEntity entityWithUpdatedInfo = pmsPipelineServiceHelper.updatePipelineInfo(pipelineEntity);
      PipelineEntity updatedResult =
          pmsPipelineRepository.updatePipelineYaml(entityWithUpdatedInfo, oldEntity, changeType);

      if (updatedResult == null) {
        throw new InvalidRequestException(format(
            "Pipeline [%s] under Project[%s], Organization [%s] couldn't be updated or doesn't exist.",
            pipelineEntity.getIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getOrgIdentifier()));
      }

      sendPipelineSaveTelemetryEvent(updatedResult, UPDATING_PIPELINE);
      return updatedResult;
    } catch (EventsFrameworkDownException ex) {
      log.error("Events framework is down for Pipeline Service.", ex);
      throw new InvalidRequestException("Error connecting to systems upstream", ex);
    } catch (IOException ex) {
      log.error(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
      throw new InvalidYamlException(format("Invalid yaml in node [%s]", YamlUtils.getErrorNodePartialFQN(ex)), ex);
    } catch (ExplanationException | ScmException e) {
      log.error("Error while updating pipeline " + pipelineEntity.getIdentifier(), e);
      throw e;
    } catch (Exception e) {
      log.error(String.format("Error while updating pipeline [%s]", pipelineEntity.getIdentifier()), e);
      throw new InvalidRequestException(String.format(
          "Error while updating pipeline [%s]: %s", pipelineEntity.getIdentifier(), ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public PipelineEntity updatePipelineMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, Criteria criteria, Update updateOperations) {
    return pmsPipelineRepository.updatePipelineMetadata(
        accountId, orgIdentifier, projectIdentifier, criteria, updateOperations);
  }

  @Override
  public void saveExecutionInfo(
      String accountId, String orgId, String projectId, String pipelineId, ExecutionSummaryInfo executionSummaryInfo) {
    Criteria criteria =
        PMSPipelineServiceHelper.getPipelineEqualityCriteria(accountId, orgId, projectId, pipelineId, false, null);

    Update update = new Update();
    update.set(PipelineEntityKeys.executionSummaryInfo, executionSummaryInfo);
    updatePipelineMetadata(accountId, orgId, projectId, criteria, update);
  }

  @Override
  public int incrementRunSequence(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, boolean deleted) {
    return pipelineMetadataService.incrementExecutionCounter(
        accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
  }

  @Override
  public int incrementRunSequence(PipelineEntity pipelineEntity) {
    String accountId = pipelineEntity.getAccountId();
    String orgIdentifier = pipelineEntity.getOrgIdentifier();
    String projectIdentifier = pipelineEntity.getProjectIdentifier();
    int count = pipelineMetadataService.incrementExecutionCounter(
        accountId, orgIdentifier, projectIdentifier, pipelineEntity.getIdentifier());
    if (count == -1) {
      try {
        PipelineMetadataV2 metadata = PipelineMetadataV2.builder()
                                          .accountIdentifier(pipelineEntity.getAccountIdentifier())
                                          .orgIdentifier(orgIdentifier)
                                          .projectIdentifier(projectIdentifier)
                                          .runSequence(pipelineEntity.getRunSequence() + 1)
                                          .identifier(pipelineEntity.getIdentifier())
                                          .build();
        return pipelineMetadataService.save(metadata).getRunSequence();
      } catch (DuplicateKeyException exception) {
        // retry insert if above fails
        return pipelineMetadataService.incrementExecutionCounter(
            accountId, orgIdentifier, projectIdentifier, pipelineEntity.getIdentifier());
      }
    }
    return count;
  }

  @Override
  public boolean markEntityInvalid(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier, String invalidYaml) {
    Optional<PipelineEntity> optionalPipelineEntity =
        get(accountIdentifier, orgIdentifier, projectIdentifier, identifier, false);
    if (!optionalPipelineEntity.isPresent()) {
      log.warn(String.format(
          "Marking pipeline [%s] as invalid failed as it does not exist or has been deleted", identifier));
      return false;
    }
    PipelineEntity existingPipeline = optionalPipelineEntity.get();
    PipelineEntity pipelineEntityUpdated = existingPipeline.withYaml(invalidYaml)
                                               .withObjectIdOfYaml(EntityObjectIdUtils.getObjectIdOfYaml(invalidYaml))
                                               .withIsEntityInvalid(true);
    pmsPipelineRepository.updatePipelineYaml(pipelineEntityUpdated, existingPipeline, ChangeType.NONE);
    return true;
  }

  @Override
  public boolean delete(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, Long version) {
    Optional<PipelineEntity> optionalPipelineEntity =
        get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!optionalPipelineEntity.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }

    PipelineEntity existingEntity = optionalPipelineEntity.get();
    PipelineEntity withDeleted = existingEntity.withDeleted(true);
    try {
      PipelineEntity deletedEntity = pmsPipelineRepository.deletePipeline(withDeleted);
      if (deletedEntity.getDeleted()) {
        return true;
      } else {
        throw new InvalidRequestException(
            format("Pipeline [%s] under Project[%s], Organization [%s] could not be deleted.", pipelineIdentifier,
                projectIdentifier, orgIdentifier));
      }
    } catch (Exception e) {
      log.error(String.format("Error while deleting pipeline [%s]", pipelineIdentifier), e);
      throw new InvalidRequestException(
          String.format("Error while deleting pipeline [%s]: %s", pipelineIdentifier, ExceptionUtils.getMessage(e)));
    }
  }

  @Override
  public Page<PipelineEntity> list(Criteria criteria, Pageable pageable, String accountId, String orgIdentifier,
      String projectIdentifier, Boolean getDistinctFromBranches) {
    if (Boolean.TRUE.equals(getDistinctFromBranches)
        && gitSyncSdkService.isGitSyncEnabled(accountId, orgIdentifier, projectIdentifier)) {
      return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, true);
    }
    return pmsPipelineRepository.findAll(criteria, pageable, accountId, orgIdentifier, projectIdentifier, false);
  }

  @Override
  public Long countAllPipelines(Criteria criteria) {
    return pmsPipelineRepository.countAllPipelines(criteria);
  }

  @Override
  public StepCategory getSteps(String module, String category, String accountId) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    StepCategory stepCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategory(
        category, serviceInstanceNameToSupportedSteps.get(module).getStepTypes(), accountId);
    for (Map.Entry<String, StepPalleteInfo> entry : serviceInstanceNameToSupportedSteps.entrySet()) {
      if (entry.getKey().equals(module) || EmptyPredicate.isEmpty(entry.getValue().getStepTypes())) {
        continue;
      }
      stepCategory.addStepCategory(pmsPipelineServiceStepHelper.calculateStepsForCategory(
          entry.getValue().getModuleName(), entry.getValue().getStepTypes(), accountId));
    }
    return stepCategory;
  }

  @Override
  public StepCategory getStepsV2(String accountId, StepPalleteFilterWrapper stepPalleteFilterWrapper) {
    Map<String, StepPalleteInfo> serviceInstanceNameToSupportedSteps =
        pmsSdkInstanceService.getModuleNameToStepPalleteInfo();
    if (stepPalleteFilterWrapper.getStepPalleteModuleInfos().isEmpty()) {
      // Return all the steps.
      return pmsPipelineServiceStepHelper.getAllSteps(accountId, serviceInstanceNameToSupportedSteps);
    }
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (StepPalleteModuleInfo request : stepPalleteFilterWrapper.getStepPalleteModuleInfos()) {
      String module = request.getModule();
      String category = request.getCategory();
      StepPalleteInfo stepPalleteInfo = serviceInstanceNameToSupportedSteps.get(module);
      if (stepPalleteInfo == null) {
        continue;
      }
      List<StepInfo> stepInfoList = stepPalleteInfo.getStepTypes();
      String displayModuleName = stepPalleteInfo.getModuleName();
      if (EmptyPredicate.isEmpty(stepInfoList)) {
        continue;
      }
      StepCategory moduleCategory;
      if (EmptyPredicate.isNotEmpty(category)) {
        moduleCategory = pmsPipelineServiceStepHelper.calculateStepsForModuleBasedOnCategoryV2(
            displayModuleName, category, stepInfoList, accountId);
      } else {
        moduleCategory =
            pmsPipelineServiceStepHelper.calculateStepsForCategory(displayModuleName, stepInfoList, accountId);
      }
      stepCategory.addStepCategory(moduleCategory);
      if (request.isShouldShowCommonSteps()) {
        pmsPipelineServiceStepHelper.addStepsToStepCategory(
            moduleCategory, commonStepInfo.getCommonSteps(request.getCommonStepCategory()), accountId);
      }
    }

    return stepCategory;
  }

  // Todo: Remove only if there are no references to the pipeline
  @Override
  public boolean deleteAllPipelinesInAProject(String accountId, String orgId, String projectId) {
    Criteria criteria = pmsPipelineServiceHelper.formCriteria(
        accountId, orgId, projectId, null, PipelineFilterPropertiesDto.builder().build(), false, null, null);
    Pageable pageRequest = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, PipelineEntityKeys.lastUpdatedAt));

    Page<PipelineEntity> pipelineEntities =
        pmsPipelineRepository.findAll(criteria, pageRequest, accountId, orgId, projectId, false);
    for (PipelineEntity pipelineEntity : pipelineEntities) {
      pmsPipelineRepository.deletePipeline(pipelineEntity.withDeleted(true));
    }
    return true;
  }

  // TODO(Brijesh): Make this async.
  private void sendPipelineSaveTelemetryEvent(PipelineEntity entity, String actionType) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PIPELINE_NAME, entity.getName());
    properties.put(ORG_ID, entity.getOrgIdentifier());
    properties.put(PROJECT_ID, entity.getProjectIdentifier());
    properties.put(PIPELINE_SAVE_ACTION_TYPE, actionType);
    properties.put(PipelineInstrumentationConstants.MODULE_NAME,
        PipelineEntityUtils.getModuleNameFromPipelineEntity(entity, "cd"));
    telemetryReporter.sendTrackEvent(PIPELINE_SAVE, null, entity.getAccountId(), properties,
        Collections.singletonMap(AMPLITUDE, true), io.harness.telemetry.Category.GLOBAL);
  }

  @Override
  public String fetchExpandedPipelineJSON(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    Optional<PipelineEntity> pipelineEntityOptional =
        get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (!pipelineEntityOptional.isPresent()) {
      throw new InvalidRequestException(PipelineCRUDErrorResponse.errorMessageForPipelineNotFound(
          orgIdentifier, projectIdentifier, pipelineIdentifier));
    }

    return pmsPipelineServiceHelper.fetchExpandedPipelineJSONFromYaml(
        accountId, orgIdentifier, projectIdentifier, pipelineEntityOptional.get().getYaml());
  }

  @Override
  public PipelineEntity updateGitFilePath(PipelineEntity pipelineEntity, String newFilePath) {
    Criteria criteria = PMSPipelineServiceHelper.getPipelineEqualityCriteria(pipelineEntity.getAccountId(),
        pipelineEntity.getOrgIdentifier(), pipelineEntity.getProjectIdentifier(), pipelineEntity.getIdentifier(), false,
        null);

    GitEntityFilePath gitEntityFilePath = GitSyncFilePathUtils.getRootFolderAndFilePath(newFilePath);
    Update update = new Update()
                        .set(PipelineEntityKeys.filePath, gitEntityFilePath.getFilePath())
                        .set(PipelineEntityKeys.rootFolder, gitEntityFilePath.getRootFolder());
    return updatePipelineMetadata(pipelineEntity.getAccountId(), pipelineEntity.getOrgIdentifier(),
        pipelineEntity.getProjectIdentifier(), criteria, update);
  }
}
