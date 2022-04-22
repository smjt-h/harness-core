/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.handler.connectors.configyamlhandlers;

import static io.harness.rule.OwnerRule.DHRUV;
import static io.harness.rule.OwnerRule.PRATYUSH;

import static software.wings.beans.CGConstants.ENCRYPTED_VALUE_STR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.GitConfig;
import software.wings.beans.GitConfig.ProviderType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.setting.sourcerepoprovider.GitConfigYamlHandler;
import software.wings.service.impl.yaml.handler.templatelibrary.SettingValueConfigYamlHandlerTestBase;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class GitConfigYamlHandlerTest extends SettingValueConfigYamlHandlerTestBase {
  @InjectMocks @Inject private GitConfigYamlHandler yamlHandler;
  @Mock private SettingsService settingsService;
  @Mock private SecretManager secretManager;
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";
  public static final String SAMPLE_STRING = "sample-string";
  private Class yamlClass = GitConfig.Yaml.class;

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testToBeanWithPassword() {
    GitConfig.Yaml yaml = GitConfig.Yaml.builder()
                              .branch(SAMPLE_STRING)
                              .reference(SAMPLE_STRING)
                              .keyAuth(true)
                              .sshKeyName(null)
                              .authorName(SAMPLE_STRING)
                              .authorEmailId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .password(SAMPLE_STRING)
                              .username(SAMPLE_STRING)
                              .url(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Source Repo Provider/test-harness.yaml")
                        .build();
    ChangeContext<GitConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.SOURCE_REPO_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    SettingAttribute settingAttribute = yamlHandler.toBean(null, changeContext, null);
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getSshSettingId()).isEqualTo(null);
    assertThat(gitConfig.getUsername()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getRepoUrl()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(Collections.emptyList());
    assertThat(gitConfig.getProviderType()).isEqualTo(ProviderType.GIT);
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testToBeanWithSshKey() {
    GitConfig.Yaml yaml = GitConfig.Yaml.builder()
                              .branch(SAMPLE_STRING)
                              .reference(SAMPLE_STRING)
                              .keyAuth(true)
                              .sshKeyName(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .authorEmailId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .username(SAMPLE_STRING)
                              .url(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Source Repo Provider/test-harness.yaml")
                        .build();
    ChangeContext<GitConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.SOURCE_REPO_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();
    doReturn(SAMPLE_STRING)
        .when(settingsService)
        .getSSHSettingId(changeContext.getChange().getAccountId(), yaml.getSshKeyName());
    SettingAttribute settingAttribute = yamlHandler.toBean(null, changeContext, null);
    GitConfig gitConfig = (GitConfig) settingAttribute.getValue();

    assertThat(gitConfig).isNotNull();
    assertThat(gitConfig.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getSshSettingId()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getPassword()).isEqualTo(null);
    assertThat(gitConfig.getUsername()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getRepoUrl()).isEqualTo(SAMPLE_STRING);
    assertThat(gitConfig.getDelegateSelectors()).isEqualTo(Collections.emptyList());
    assertThat(gitConfig.getProviderType()).isEqualTo(ProviderType.GIT);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testToBeanWithSSHKeyNameAndPassword() {
    GitConfig.Yaml yaml = GitConfig.Yaml.builder()
                              .branch(SAMPLE_STRING)
                              .reference(SAMPLE_STRING)
                              .keyAuth(true)
                              .sshKeyName(SAMPLE_STRING)
                              .password(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .authorEmailId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .username(SAMPLE_STRING)
                              .url(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Source Repo Provider/test-harness.yaml")
                        .build();
    ChangeContext<GitConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.SOURCE_REPO_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    doReturn(SAMPLE_STRING).when(settingsService).getSSHSettingId("ABC", yaml.getSshKeyName());
    assertThatThrownBy(() -> yamlHandler.toBean(null, changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex -> ex.getMessage().equals("Cannot use both the encryption types SSH key and password at once"));
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testToBeanWithoutSSHKeyNameAndPassword() {
    GitConfig.Yaml yaml = GitConfig.Yaml.builder()
                              .branch(SAMPLE_STRING)
                              .reference(SAMPLE_STRING)
                              .keyAuth(true)
                              .sshKeyName(null)
                              .authorName(SAMPLE_STRING)
                              .authorEmailId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .password(ENCRYPTED_VALUE_STR)
                              .username(SAMPLE_STRING)
                              .url(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    Change change = Change.Builder.aFileChange()
                        .withAccountId("ABC")
                        .withFilePath("Setup/Source Repo Provider/test-harness.yaml")
                        .build();
    ChangeContext<GitConfig.Yaml> changeContext = ChangeContext.Builder.aChangeContext()
                                                      .withYamlType(YamlType.SOURCE_REPO_PROVIDER)
                                                      .withYaml(yaml)
                                                      .withChange(change)
                                                      .build();

    assertThatThrownBy(() -> yamlHandler.toBean(null, changeContext, null))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex -> ex.getMessage().equals("Both SSH key and password cannot be null"));
  }

  @Test
  @Owner(developers = DHRUV)
  @Category(UnitTests.class)
  public void testToYamlWithPassword() {
    GitConfig gitConfig = GitConfig.builder()
                              .authorEmailId(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .accountId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .branch(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .sshSettingId(null)
                              .encryptedPassword(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    SettingValue settingValue = (SettingValue) gitConfig;
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SAMPLE_STRING)
                                            .withUuid(null)
                                            .withValue(settingValue)
                                            .build();
    doReturn(SAMPLE_STRING)
        .when(secretManager)
        .getEncryptedYamlRef(gitConfig.getAccountId(), gitConfig.getEncryptedPassword());
    GitConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, null);

    assertThat(yaml).isNotNull();
    assertThat(yaml.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getDescription()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getPassword()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getSshKeyName()).isEqualTo(null);
    assertThat(yaml.getDelegateSelectors()).isEqualTo(Collections.emptyList());
    assertThat(yaml.getProviderType()).isEqualTo(ProviderType.GIT);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testToYamlWithSshKey() {
    GitConfig gitConfig = GitConfig.builder()
                              .authorEmailId(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .accountId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .branch(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .sshSettingId(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    SettingValue settingValue = (SettingValue) gitConfig;
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SAMPLE_STRING)
                                            .withUuid(null)
                                            .withValue(settingValue)
                                            .build();
    doReturn(SAMPLE_STRING).when(settingsService).getSSHKeyName(gitConfig.getSshSettingId());
    GitConfig.Yaml yaml = yamlHandler.toYaml(settingAttribute, null);

    assertThat(yaml).isNotNull();
    assertThat(yaml.getCommitMessage()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorEmailId()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getAuthorName()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getBranch()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getDescription()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getPassword()).isEqualTo(null);
    assertThat(yaml.getSshKeyName()).isEqualTo(SAMPLE_STRING);
    assertThat(yaml.getDelegateSelectors()).isEqualTo(Collections.emptyList());
    assertThat(yaml.getProviderType()).isEqualTo(ProviderType.GIT);
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testToYamlWithSshKeyAndPassword() {
    GitConfig gitConfig = GitConfig.builder()
                              .authorEmailId(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .accountId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .branch(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .encryptedPassword(SAMPLE_STRING)
                              .sshSettingId(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    SettingValue settingValue = (SettingValue) gitConfig;
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SAMPLE_STRING)
                                            .withUuid(null)
                                            .withValue(settingValue)
                                            .build();
    doReturn(SAMPLE_STRING).when(settingsService).getSSHKeyName(gitConfig.getSshSettingId());
    doReturn(SAMPLE_STRING)
        .when(secretManager)
        .getEncryptedYamlRef(gitConfig.getAccountId(), gitConfig.getEncryptedPassword());
    assertThatThrownBy(() -> yamlHandler.toYaml(settingAttribute, null))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex -> ex.getMessage().equals("Cannot use both the encryption types SSH key and password at once"));
  }

  @Test
  @Owner(developers = PRATYUSH)
  @Category(UnitTests.class)
  public void testToYamlWithoutSshKeyAndPassword() {
    GitConfig gitConfig = GitConfig.builder()
                              .authorEmailId(SAMPLE_STRING)
                              .authorName(SAMPLE_STRING)
                              .accountId(SAMPLE_STRING)
                              .commitMessage(SAMPLE_STRING)
                              .branch(SAMPLE_STRING)
                              .description(SAMPLE_STRING)
                              .delegateSelectors(Collections.emptyList())
                              .providerType(ProviderType.GIT)
                              .build();

    SettingValue settingValue = (SettingValue) gitConfig;
    SettingAttribute settingAttribute = SettingAttribute.Builder.aSettingAttribute()
                                            .withName(SAMPLE_STRING)
                                            .withUuid(null)
                                            .withValue(settingValue)
                                            .build();
    doReturn(SAMPLE_STRING).when(settingsService).getSSHKeyName(gitConfig.getSshSettingId());
    doReturn(SAMPLE_STRING)
        .when(secretManager)
        .getEncryptedYamlRef(gitConfig.getAccountId(), gitConfig.getEncryptedPassword());
    assertThatThrownBy(() -> yamlHandler.toYaml(settingAttribute, null))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex -> ex.getMessage().equals("Both SSH key and password cannot be null"));
  }
}
