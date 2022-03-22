package io.harness.secrets.validation;

import static io.harness.beans.SecretManagerCapabilities.CREATE_FILE_SECRET;
import static io.harness.beans.SecretManagerCapabilities.CREATE_INLINE_SECRET;
import static io.harness.eraro.ErrorCode.GCP_SECRET_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.rule.OwnerRule.VIKAS_M;
import static io.harness.security.SimpleEncryption.CHARSET;

import static software.wings.settings.SettingVariableTypes.CONFIG_FILE;
import static software.wings.settings.SettingVariableTypes.SECRET_TEXT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.EncryptedData;
import io.harness.beans.HarnessSecret;
import io.harness.beans.SecretFile;
import io.harness.beans.SecretManagerConfig;
import io.harness.beans.SecretText;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.SecretManagementException;
import io.harness.rule.Owner;
import io.harness.secrets.SecretsDao;
import io.harness.secrets.validation.validators.GcpSecretManagerValidator;
import io.harness.security.encryption.EncryptedDataParams;
import io.harness.security.encryption.EncryptionType;

import com.google.common.collect.Sets;
import java.security.SecureRandom;
import java.util.Optional;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GcpSecretManagerValidatorTest extends CategoryTest {
  private GcpSecretManagerValidator gcpSecretManagerValidator;
  private SecretsDao secretsDao;

  @Before
  public void setup() {
    secretsDao = mock(SecretsDao.class);
    gcpSecretManagerValidator = new GcpSecretManagerValidator(secretsDao);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateInlineSecret_shouldPass() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    HarnessSecret secret = SecretText.builder().name(name).kmsId(accountId).value(UUIDGenerator.generateUuid()).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_INLINE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    gcpSecretManagerValidator.validateSecret(accountId, secret, secretManagerConfig);
    verify(secretsDao, times(1)).getSecretByName(accountId, name);
    verify(secretManagerConfig, times(1)).getSecretManagerCapabilities();
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateInlineSecretText_invalidName_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = UUIDGenerator.generateUuid() + "]";
    HarnessSecret secret = SecretText.builder().name(name).kmsId(accountId).value(UUIDGenerator.generateUuid()).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_INLINE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    try {
      gcpSecretManagerValidator.validateSecret(accountId, secret, secretManagerConfig);
      fail("Invalid characters in the name should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage())
          .isEqualTo(
              "Secret names can only contain English letters (A-Z), numbers (0-9), dashes (-), and underscores (_)");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretTextUpdate_shouldPass() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    String kmsId = UUIDGenerator.generateUuid();
    SecretText secret = SecretText.builder().name(name).kmsId(kmsId).value(UUIDGenerator.generateUuid()).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name(UUIDGenerator.generateUuid())
                                       .type(SECRET_TEXT)
                                       .encryptionType(EncryptionType.GCP_SECRETS_MANAGER)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .parameters(Sets.newHashSet(EncryptedDataParams.builder()
                                                                       .name(UUIDGenerator.generateUuid())
                                                                       .value(UUIDGenerator.generateUuid())
                                                                       .build()))
                                       .build();

    gcpSecretManagerValidator.validateSecretTextUpdate(secret, existingRecord, secretManagerConfig);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretTextUpdate_invalidName_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = UUIDGenerator.generateUuid() + "]";
    String kmsId = UUIDGenerator.generateUuid();
    SecretText secret = SecretText.builder().name(name).kmsId(kmsId).value(UUIDGenerator.generateUuid()).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name(UUIDGenerator.generateUuid())
                                       .type(SECRET_TEXT)
                                       .encryptionType(EncryptionType.GCP_SECRETS_MANAGER)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .parameters(Sets.newHashSet(EncryptedDataParams.builder()
                                                                       .name(UUIDGenerator.generateUuid())
                                                                       .value(UUIDGenerator.generateUuid())
                                                                       .build()))
                                       .build();
    try {
      gcpSecretManagerValidator.validateSecretTextUpdate(secret, existingRecord, secretManagerConfig);
      fail("Invalid characters in the name should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage())
          .isEqualTo(
              "Secret names can only contain English letters (A-Z), numbers (0-9), dashes (-), and underscores (_)");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretTextUpdate_changingTypeOfSecret_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = UUIDGenerator.generateUuid() + "]";
    String kmsId = UUIDGenerator.generateUuid();
    SecretText secret =
        SecretText.builder().name(name).kmsId(kmsId).path("path").value(UUIDGenerator.generateUuid()).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name(UUIDGenerator.generateUuid())
                                       .type(SECRET_TEXT)
                                       .encryptionType(EncryptionType.GCP_SECRETS_MANAGER)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .parameters(Sets.newHashSet(EncryptedDataParams.builder()
                                                                       .name(UUIDGenerator.generateUuid())
                                                                       .value(UUIDGenerator.generateUuid())
                                                                       .build()))
                                       .build();
    try {
      gcpSecretManagerValidator.validateSecretTextUpdate(secret, existingRecord, secretManagerConfig);
      fail("Changing the type of secret should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(SECRET_MANAGEMENT_ERROR);
      assertThat(e.getMessage()).isEqualTo("Cannot change the type of secret");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateEncryptedFile_shouldPass() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    HarnessSecret secret = SecretFile.builder()
                               .name(name)
                               .kmsId(accountId)
                               .fileContent(UUIDGenerator.generateUuid().getBytes(CHARSET))
                               .build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_FILE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    gcpSecretManagerValidator.validateSecret(accountId, secret, secretManagerConfig);
    verify(secretsDao, times(1)).getSecretByName(accountId, name);
    verify(secretManagerConfig, times(1)).getSecretManagerCapabilities();
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateEncryptedFile_invalidName_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = UUIDGenerator.generateUuid() + "]";
    HarnessSecret secret = SecretFile.builder()
                               .name(name)
                               .kmsId(accountId)
                               .fileContent(UUIDGenerator.generateUuid().getBytes(CHARSET))
                               .build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_FILE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    try {
      gcpSecretManagerValidator.validateSecret(accountId, secret, secretManagerConfig);
      fail("Invalid characters in the name should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage())
          .isEqualTo(
              "Secret names can only contain English letters (A-Z), numbers (0-9), dashes (-), and underscores (_)");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testCreateEncryptedFile_exceedFileLimits_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    SecureRandom secureRandom = new SecureRandom();
    byte[] bytes = new byte[65537];
    secureRandom.nextBytes(bytes);
    HarnessSecret secret = SecretFile.builder().name(name).kmsId(accountId).fileContent(bytes).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_FILE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    try {
      gcpSecretManagerValidator.validateSecret(accountId, secret, secretManagerConfig);
      fail("File size greater, should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage()).isEqualTo("File size limit is 64 KiB");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretFileUpdate_shouldPass() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    String kmsId = UUIDGenerator.generateUuid();
    HarnessSecret secret = SecretFile.builder()
                               .name(name)
                               .kmsId(kmsId)
                               .fileContent(UUIDGenerator.generateUuid().getBytes(CHARSET))
                               .build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name("onlyalphanumeric123")
                                       .type(CONFIG_FILE)
                                       .encryptionType(EncryptionType.KMS)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .build();
    gcpSecretManagerValidator.validateSecretUpdate(secret, existingRecord, secretManagerConfig);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretFileUpdate_secretNameModified_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    String kmsId = UUIDGenerator.generateUuid();
    HarnessSecret secret = SecretFile.builder()
                               .name(name)
                               .kmsId(kmsId)
                               .fileContent(UUIDGenerator.generateUuid().getBytes(CHARSET))
                               .build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_FILE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name(UUIDGenerator.generateUuid())
                                       .type(CONFIG_FILE)
                                       .encryptionType(EncryptionType.KMS)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .build();
    try {
      gcpSecretManagerValidator.validateSecretUpdate(secret, existingRecord, secretManagerConfig);
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage()).isEqualTo("Renaming Secrets in GCP Secret Manager is not supported");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretFileUpdate_invalidName_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = UUIDGenerator.generateUuid() + "]";
    String kmsId = UUIDGenerator.generateUuid();
    HarnessSecret secret = SecretFile.builder()
                               .name(name)
                               .kmsId(kmsId)
                               .fileContent(UUIDGenerator.generateUuid().getBytes(CHARSET))
                               .build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretManagerConfig.getSecretManagerCapabilities()).thenReturn(Lists.list(CREATE_FILE_SECRET));
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name(name)
                                       .type(CONFIG_FILE)
                                       .encryptionType(EncryptionType.KMS)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .build();
    try {
      gcpSecretManagerValidator.validateSecretUpdate(secret, existingRecord, secretManagerConfig);
      fail("Invalid characters in the name should have failed");
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage())
          .isEqualTo(
              "Secret names can only contain English letters (A-Z), numbers (0-9), dashes (-), and underscores (_)");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testSecretFileUpdate_fileSizeCheck_shouldThrowError() {
    String accountId = UUIDGenerator.generateUuid();
    String name = "onlyalphanumeric123";
    String kmsId = UUIDGenerator.generateUuid();
    SecureRandom secureRandom = new SecureRandom();
    byte[] bytes = new byte[24001];
    secureRandom.nextBytes(bytes);
    HarnessSecret secret = SecretFile.builder().name(name).kmsId(kmsId).fileContent(bytes).build();
    SecretManagerConfig secretManagerConfig = mock(SecretManagerConfig.class);
    when(secretsDao.getSecretByName(accountId, name)).thenReturn(Optional.empty());
    EncryptedData existingRecord = EncryptedData.builder()
                                       .name("onlyalphanumeric123")
                                       .type(CONFIG_FILE)
                                       .encryptionType(EncryptionType.KMS)
                                       .accountId(accountId)
                                       .kmsId(kmsId)
                                       .build();
    try {
      gcpSecretManagerValidator.validateSecretUpdate(secret, existingRecord, secretManagerConfig);
    } catch (SecretManagementException e) {
      assertThat(e.getCode()).isEqualTo(GCP_SECRET_OPERATION_ERROR);
      assertThat(e.getMessage()).isEqualTo("File size limit is 64 KiB");
    }
  }
}
