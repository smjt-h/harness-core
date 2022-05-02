/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.engine.GovernanceService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorDTO;
import io.harness.exception.ngexception.beans.yamlschema.YamlSchemaErrorWrapperDTO;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterDTO;
import io.harness.filter.service.FilterService;
import io.harness.gitsync.helpers.GitContextHelper;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.ng.core.template.TemplateMergeResponseDTO;
import io.harness.opaclient.model.OpaConstants;
import io.harness.pms.PmsFeatureFlagService;
import io.harness.pms.contracts.governance.ExpansionRequestMetadata;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.GovernanceMetadata;
import io.harness.pms.filter.creation.FilterCreatorMergeService;
import io.harness.pms.filter.creation.FilterCreatorMergeServiceResponse;
import io.harness.pms.filter.utils.ModuleInfoFilterUtils;
import io.harness.pms.gitsync.PmsGitSyncBranchContextGuard;
import io.harness.pms.gitsync.PmsGitSyncHelper;
import io.harness.pms.governance.ExpansionRequest;
import io.harness.pms.governance.ExpansionRequestsExtractor;
import io.harness.pms.governance.ExpansionsMerger;
import io.harness.pms.governance.JsonExpander;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.PipelineEntity.PipelineEntityKeys;
import io.harness.pms.pipeline.PipelineFilterPropertiesDto;
import io.harness.serializer.JsonUtils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PIPELINE)
public class PMSPipelineServiceHelper {
  @Inject private final FilterService filterService;
  @Inject private final FilterCreatorMergeService filterCreatorMergeService;
  @Inject private final PMSYamlSchemaService pmsYamlSchemaService;
  @Inject private final PMSPipelineTemplateHelper pipelineTemplateHelper;
  @Inject private final GovernanceService governanceService;
  @Inject private final JsonExpander jsonExpander;
  @Inject private final ExpansionRequestsExtractor expansionRequestsExtractor;
  @Inject private final PmsFeatureFlagService pmsFeatureFlagService;
  @Inject private final PmsGitSyncHelper gitSyncHelper;

  public static void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  public static Criteria getPipelineEqualityCriteria(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, boolean deleted, Long version) {
    Criteria criteria = Criteria.where(PipelineEntityKeys.accountId)
                            .is(accountId)
                            .and(PipelineEntityKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(PipelineEntityKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(PipelineEntityKeys.identifier)
                            .is(pipelineIdentifier)
                            .and(PipelineEntityKeys.deleted)
                            .is(deleted);

    if (version != null) {
      criteria.and(PipelineEntityKeys.version).is(version);
    }

    return criteria;
  }

  public PipelineEntity updatePipelineInfo(PipelineEntity pipelineEntity) throws IOException {
    FilterCreatorMergeServiceResponse filtersAndStageCount = filterCreatorMergeService.getPipelineInfo(pipelineEntity);
    PipelineEntity newEntity = pipelineEntity.withStageCount(filtersAndStageCount.getStageCount())
                                   .withStageNames(filtersAndStageCount.getStageNames());
    newEntity.getFilters().clear();
    if (isNotEmpty(filtersAndStageCount.getFilters())) {
      filtersAndStageCount.getFilters().forEach((key, value) -> newEntity.getFilters().put(key, Document.parse(value)));
    }
    return newEntity;
  }

  public void populateFilterUsingIdentifier(Criteria criteria, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, @NotNull String filterIdentifier) {
    FilterDTO pipelineFilterDTO = filterService.get(
        accountIdentifier, orgIdentifier, projectIdentifier, filterIdentifier, FilterType.PIPELINESETUP);
    if (pipelineFilterDTO == null) {
      throw new InvalidRequestException("Could not find a pipeline filter with the identifier ");
    } else {
      populateFilter(criteria, (PipelineFilterPropertiesDto) pipelineFilterDTO.getFilterProperties());
    }
  }

  public static void populateFilter(Criteria criteria, @NotNull PipelineFilterPropertiesDto pipelineFilter) {
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getName())) {
      criteria.and(PipelineEntityKeys.name).is(pipelineFilter.getName());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getDescription())) {
      criteria.and(PipelineEntityKeys.description).is(pipelineFilter.getDescription());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getPipelineTags())) {
      criteria.and(PipelineEntityKeys.tags).in(pipelineFilter.getPipelineTags());
    }
    if (EmptyPredicate.isNotEmpty(pipelineFilter.getPipelineIdentifiers())) {
      criteria.and(PipelineEntityKeys.identifier).in(pipelineFilter.getPipelineIdentifiers());
    }
    if (pipelineFilter.getModuleProperties() != null) {
      ModuleInfoFilterUtils.processNode(
          JsonUtils.readTree(pipelineFilter.getModuleProperties().toJson()), "filters", criteria);
    }
  }

  public GovernanceMetadata validatePipelineYamlAndSetTemplateRefIfAny(
      PipelineEntity pipelineEntity, boolean checkAgainstOPAPolicies) {
    try {
      GitEntityInfo gitEntityInfo = GitContextHelper.getGitEntityInfo();
      if (gitEntityInfo != null && gitEntityInfo.isNewBranch()) {
        GitSyncBranchContext gitSyncBranchContext =
            GitSyncBranchContext.builder()
                .gitBranchInfo(GitEntityInfo.builder()
                                   .branch(gitEntityInfo.getBaseBranch())
                                   .yamlGitConfigId(gitEntityInfo.getYamlGitConfigId())
                                   .build())
                .build();
        try (PmsGitSyncBranchContextGuard ignored = new PmsGitSyncBranchContextGuard(gitSyncBranchContext, true)) {
          return validatePipelineYamlAndSetTemplateRefIfAnyInternal(pipelineEntity, checkAgainstOPAPolicies);
        }
      } else {
        return validatePipelineYamlAndSetTemplateRefIfAnyInternal(pipelineEntity, checkAgainstOPAPolicies);
      }
    } catch (io.harness.yaml.validator.InvalidYamlException ex) {
      ex.setYaml(pipelineEntity.getData());
      throw ex;
    } catch (Exception ex) {
      YamlSchemaErrorWrapperDTO errorWrapperDTO =
          YamlSchemaErrorWrapperDTO.builder()
              .schemaErrors(Collections.singletonList(
                  YamlSchemaErrorDTO.builder().message(ex.getMessage()).fqn("$.pipeline").build()))
              .build();
      throw new io.harness.yaml.validator.InvalidYamlException(
          HarnessStringUtils.emptyIfNull(ex.getMessage()), ex, errorWrapperDTO, pipelineEntity.getData());
    }
  }

  private GovernanceMetadata validatePipelineYamlAndSetTemplateRefIfAnyInternal(
      PipelineEntity pipelineEntity, boolean checkAgainstOPAPolicies) {
    String accountId = pipelineEntity.getAccountId();
    String orgIdentifier = pipelineEntity.getOrgIdentifier();
    String projectIdentifier = pipelineEntity.getProjectIdentifier();
    // Apply all the templateRefs(if any) then check for schema validation.
    TemplateMergeResponseDTO templateMergeResponseDTO =
        pipelineTemplateHelper.resolveTemplateRefsInPipeline(pipelineEntity);
    String resolveTemplateRefsInPipeline = templateMergeResponseDTO.getMergedPipelineYaml();
    pmsYamlSchemaService.validateYamlSchema(accountId, orgIdentifier, projectIdentifier, resolveTemplateRefsInPipeline);
    // validate unique fqn in resolveTemplateRefsInPipeline
    pmsYamlSchemaService.validateUniqueFqn(resolveTemplateRefsInPipeline);
    pipelineEntity.setTemplateReference(
        EmptyPredicate.isNotEmpty(templateMergeResponseDTO.getTemplateReferenceSummaries()));
    if (checkAgainstOPAPolicies) {
      String expandedPipelineJSON =
          fetchExpandedPipelineJSONFromYaml(accountId, orgIdentifier, projectIdentifier, resolveTemplateRefsInPipeline);
      return governanceService.evaluateGovernancePolicies(expandedPipelineJSON, accountId, orgIdentifier,
          projectIdentifier, OpaConstants.OPA_EVALUATION_ACTION_PIPELINE_SAVE, "");
    }
    return GovernanceMetadata.newBuilder().setDeny(false).build();
  }

  public String fetchExpandedPipelineJSONFromYaml(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineYaml) {
    if (!pmsFeatureFlagService.isEnabled(accountId, FeatureName.OPA_PIPELINE_GOVERNANCE)) {
      return pipelineYaml;
    }
    long start = System.currentTimeMillis();
    ExpansionRequestMetadata expansionRequestMetadata =
        getRequestMetadata(accountId, orgIdentifier, projectIdentifier, pipelineYaml);

    Set<ExpansionRequest> expansionRequests = expansionRequestsExtractor.fetchExpansionRequests(pipelineYaml);
    Set<ExpansionResponseBatch> expansionResponseBatches =
        jsonExpander.fetchExpansionResponses(expansionRequests, expansionRequestMetadata);
    String mergeExpansions = ExpansionsMerger.mergeExpansions(pipelineYaml, expansionResponseBatches);
    log.info("[PMS_GOVERNANCE] Pipeline Json Expansion took {}ms for projectId {}, orgId {}, accountId {}",
        System.currentTimeMillis() - start, projectIdentifier, orgIdentifier, accountId);
    return mergeExpansions;
  }

  ExpansionRequestMetadata getRequestMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineYaml) {
    ByteString gitSyncBranchContextBytes = gitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    ExpansionRequestMetadata.Builder expansionRequestMetadataBuilder =
        ExpansionRequestMetadata.newBuilder()
            .setAccountId(accountId)
            .setOrgId(orgIdentifier)
            .setProjectId(projectIdentifier)
            .setYaml(ByteString.copyFromUtf8(pipelineYaml));
    if (gitSyncBranchContextBytes != null) {
      expansionRequestMetadataBuilder.setGitSyncBranchContext(gitSyncBranchContextBytes);
    }
    return expansionRequestMetadataBuilder.build();
  }
}
