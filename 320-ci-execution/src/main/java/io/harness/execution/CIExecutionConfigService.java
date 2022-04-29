/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution;

import io.harness.beans.steps.CIStepInfoType;
import io.harness.ci.beans.entities.CIExecutionConfig;
import io.harness.ci.beans.entities.CIExecutionImages;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.config.CIStepConfig;
import io.harness.ci.config.StepImageConfig;
import io.harness.repositories.CIExecutionConfigRepository;

import com.google.inject.Inject;
import de.skuzzle.semantic.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CIExecutionConfigService {
  @Inject CIExecutionConfigRepository configRepository;
  @Inject CIExecutionServiceConfig ciExecutionServiceConfig;

  public CIExecutionServiceConfig getCiExecutionServiceConfig() {
    return ciExecutionServiceConfig;
  }

  public Boolean updateCIContainerTag(String accountId, CIExecutionImages ciExecutionImages) {
    CIExecutionConfig executionConfig;
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    if (existingConfig.isPresent()) {
      executionConfig = existingConfig.get();
    } else {
      executionConfig = CIExecutionConfig.builder().accountIdentifier(accountId).build();
    }
    executionConfig.setGitCloneImage(ciExecutionImages.getGitCloneTag());
    executionConfig.setAddOnImage(ciExecutionImages.getAddonTag());
    executionConfig.setLiteEngineImage(ciExecutionImages.getLiteEngineTag());
    executionConfig.setArtifactoryUploadTag(ciExecutionImages.getArtifactoryUploadTag());
    executionConfig.setBuildAndPushDockerRegistryImage(ciExecutionImages.getBuildAndPushDockerRegistryTag());
    executionConfig.setCacheGCSTag(ciExecutionImages.getCacheGCSTag());
    executionConfig.setCacheS3Tag(ciExecutionImages.getCacheS3Tag());
    executionConfig.setBuildAndPushECRImage(ciExecutionImages.getBuildAndPushECRTag());
    executionConfig.setBuildAndPushGCRImage(ciExecutionImages.getBuildAndPushGCRTag());
    executionConfig.setGcsUploadImage(ciExecutionImages.getGcsUploadTag());
    executionConfig.setS3UploadImage(ciExecutionImages.getS3UploadTag());
    executionConfig.setSecurityImage(ciExecutionImages.getSecurityTag());
    configRepository.save(executionConfig);
    return true;
  }

  public String getAddonImage(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String image;
    if (configOptional.isPresent()) {
      image = configOptional.get().getAddOnImage();
    } else {
      image = ciExecutionServiceConfig.getAddonImage();
    }
    return image;
  }

  public String getLiteEngineImage(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    String image;
    if (configOptional.isPresent()) {
      image = configOptional.get().getLiteEngineImage();
    } else {
      image = ciExecutionServiceConfig.getLiteEngineImage();
    }
    return image;
  }

  public CIExecutionImages getCurrentConfig(String accountId) {
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    if (existingConfig.isPresent()) {
      CIExecutionConfig config = existingConfig.get();
      return CIExecutionImages.builder()
          .buildAndPushDockerRegistryTag(config.getBuildAndPushDockerRegistryImage())
          .addonTag(config.getAddOnImage())
          .liteEngineTag(config.getLiteEngineImage())
          .gitCloneTag(config.getGitCloneImage())
          .buildAndPushECRTag(config.getBuildAndPushECRImage())
          .buildAndPushGCRTag(config.getBuildAndPushGCRImage())
          .gcsUploadTag(config.getGcsUploadImage())
          .s3UploadTag(config.getS3UploadImage())
          .artifactoryUploadTag(config.getArtifactoryUploadTag())
          .cacheGCSTag(config.getCacheGCSTag())
          .cacheS3Tag(config.getCacheS3Tag())
          .securityTag(config.getSecurityImage())
          .build();
    } else {
      return CIExecutionImages.builder().build();
    }
  }

  public CIExecutionImages getDefaultConfig() {
    CIStepConfig config = ciExecutionServiceConfig.getStepConfig();
    return CIExecutionImages.builder()
        .buildAndPushDockerRegistryTag(config.getBuildAndPushDockerRegistryConfig().getImage())
        .addonTag(ciExecutionServiceConfig.getAddonImage())
        .liteEngineTag(ciExecutionServiceConfig.getLiteEngineImage())
        .gitCloneTag(config.getGitCloneConfig().getImage())
        .buildAndPushECRTag(config.getBuildAndPushECRConfig().getImage())
        .buildAndPushGCRTag(config.getBuildAndPushGCRConfig().getImage())
        .gcsUploadTag(config.getGcsUploadConfig().getImage())
        .s3UploadTag(config.getS3UploadConfig().getImage())
        .artifactoryUploadTag(config.getArtifactoryUploadConfig().getImage())
        .cacheGCSTag(config.getCacheGCSConfig().getImage())
        .cacheS3Tag(config.getCacheS3Config().getImage())
        .securityTag(config.getSecurityConfig().getImage())
        .build();
  }

  public List<DeprecatedImageInfo> getDeprecatedTags(String accountId) {
    Optional<CIExecutionConfig> configOptional = configRepository.findFirstByAccountIdentifier(accountId);
    List<DeprecatedImageInfo> deprecatedTags = new ArrayList();
    if (configOptional.isPresent()) {
      CIExecutionConfig ciExecutionConfig = configOptional.get();
      if (!checkForCIImage(ciExecutionServiceConfig.getAddonImage(), ciExecutionConfig.getAddOnImage())) {
        deprecatedTags.add(
            DeprecatedImageInfo.builder().tag("AddonImage").version(ciExecutionConfig.getAddOnImage()).build());
      }
      if (!checkForCIImage(ciExecutionServiceConfig.getLiteEngineImage(), ciExecutionConfig.getLiteEngineImage())) {
        deprecatedTags.add(DeprecatedImageInfo.builder()
                               .tag("LiteEngineImage")
                               .version(ciExecutionConfig.getLiteEngineImage())
                               .build());
      }
    }
    return deprecatedTags;
  }

  private boolean checkForCIImage(String defaultImage, String customImage) {
    String defaultImageTag = defaultImage.split(":")[1];
    String customImageTag = customImage.split(":")[1];
    Version defaultVersion = Version.parseVersion(defaultImageTag);
    Version customVersion = Version.parseVersion(customImageTag);
    // we are supporting 2 back versions
    return defaultVersion.isLowerThanOrEqualTo(customVersion.nextMinor().nextMinor());
  }

  public StepImageConfig getPluginVersion(CIStepInfoType stepInfoType, String accountId) {
    Optional<CIExecutionConfig> existingConfig = configRepository.findFirstByAccountIdentifier(accountId);
    List<String> entrypoint;
    String image;
    switch (stepInfoType) {
      case DOCKER:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushDockerRegistryImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushDockerRegistryConfig().getImage();
        }
        break;
      case GCR:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushGCRImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushGCRConfig().getImage();
        }
        break;
      case ECR:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getBuildAndPushECRImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getBuildAndPushECRConfig().getImage();
        }
        break;
      case RESTORE_CACHE_S3:
      case SAVE_CACHE_S3:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getCacheS3Config().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getCacheS3Tag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getCacheS3Config().getImage();
        }
        break;
      case UPLOAD_S3:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getS3UploadImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getS3UploadConfig().getImage();
        }
        break;
      case UPLOAD_GCS:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getGcsUploadImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getGcsUploadConfig().getImage();
        }
        break;
      case SAVE_CACHE_GCS:
      case RESTORE_CACHE_GCS:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getCacheGCSTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getCacheGCSConfig().getImage();
        }
        break;
      case SECURITY:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getSecurityConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getSecurityImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getSecurityConfig().getImage();
        }
        break;
      case UPLOAD_ARTIFACTORY:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getArtifactoryUploadTag();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getArtifactoryUploadConfig().getImage();
        }
        break;
      case GIT_CLONE:
        entrypoint = ciExecutionServiceConfig.getStepConfig().getGitCloneConfig().getEntrypoint();
        if (existingConfig.isPresent()) {
          image = existingConfig.get().getGitCloneImage();
        } else {
          image = ciExecutionServiceConfig.getStepConfig().getGitCloneConfig().getImage();
        }
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + stepInfoType);
    }
    return StepImageConfig.builder().entrypoint(entrypoint).image(image).build();
  }

  public Boolean deleteCIExecutionConfig(String accountIdentifier) {
    Optional<CIExecutionConfig> executionConfig = configRepository.findFirstByAccountIdentifier(accountIdentifier);
    executionConfig.ifPresent(ciExecutionConfig -> configRepository.deleteById(ciExecutionConfig.getUuid()));
    return true;
  }
}
