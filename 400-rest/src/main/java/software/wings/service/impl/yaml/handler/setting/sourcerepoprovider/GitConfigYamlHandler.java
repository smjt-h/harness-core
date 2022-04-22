/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.setting.sourcerepoprovider;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import static software.wings.beans.CGConstants.ENCRYPTED_VALUE_STR;

import static java.util.Objects.isNull;
import static org.apache.commons.lang.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.Yaml;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;

import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class GitConfigYamlHandler extends SourceRepoProviderYamlHandler<Yaml, GitConfig> {
  @Override
  public Yaml toYaml(SettingAttribute settingAttribute, String appId) {
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
    String password = !isNull(gitConfig.getEncryptedPassword())
        ? getEncryptedYamlRef(gitConfig.getAccountId(), gitConfig.getEncryptedPassword())
        : null;
    String sshKeyName =
        !isNull(gitConfig.getSshSettingId()) ? settingsService.getSSHKeyName(gitConfig.getSshSettingId()) : null;
    if (isNull(password) && isNull(sshKeyName)) {
      throw new InvalidRequestException("Both SSH key and password cannot be null", USER);
    } else if (!isNull(password) && !isNull(sshKeyName)) {
      throw new InvalidRequestException("Cannot use both the encryption types SSH key and password at once", USER);
    }

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .type(gitConfig.getType())
                    .url(gitConfig.getRepoUrl())
                    .username(gitConfig.getUsername())
                    .password(password)
                    .branch(gitConfig.getBranch())
                    .keyAuth(gitConfig.isKeyAuth())
                    .sshKeyName(sshKeyName)
                    .description(gitConfig.getDescription())
                    .authorName(gitConfig.getAuthorName())
                    .authorEmailId(gitConfig.getAuthorEmailId())
                    .commitMessage(gitConfig.getCommitMessage())
                    .urlType(gitConfig.getUrlType())
                    .delegateSelectors(gitConfig.getDelegateSelectors())
                    .providerType(gitConfig.getProviderType())
                    .build();
    toYaml(yaml, settingAttribute, appId);
    return yaml;
  }

  @Override
  public SettingAttribute toBean(
      SettingAttribute previous, ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String uuid = previous != null ? previous.getUuid() : null;
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String password = yaml.getPassword();
    String sshSettingId =
        !isNull(yaml.getSshKeyName()) ? settingsService.getSSHSettingId(accountId, yaml.getSshKeyName()) : null;
    if (checkForEmptyPassword(password) && isNull(sshSettingId)) {
      throw new InvalidRequestException("Both SSH key and password cannot be null", USER);
    } else if (!checkForEmptyPassword(password) && !isNull(sshSettingId)) {
      throw new InvalidRequestException("Cannot use both the encryption types SSH key and password at once", USER);
    }

    GitConfig config = GitConfig.builder()
                           .accountId(accountId)
                           .repoUrl(yaml.getUrl())
                           .branch(yaml.getBranch())
                           .encryptedPassword(password)
                           .username(yaml.getUsername())
                           .keyAuth(yaml.isKeyAuth())
                           .sshSettingId(sshSettingId)
                           .authorName(yaml.getAuthorName())
                           .authorEmailId(yaml.getAuthorEmailId())
                           .commitMessage(yaml.getCommitMessage())
                           .urlType(yaml.getUrlType())
                           .delegateSelectors(yaml.getDelegateSelectors())
                           .providerType(yaml.getProviderType())
                           .build();
    return buildSettingAttribute(accountId, changeContext.getChange().getFilePath(), uuid, config);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  public boolean checkForEmptyPassword(String password) {
    return isBlank(password) || password.equals(ENCRYPTED_VALUE_STR);
  }
}
