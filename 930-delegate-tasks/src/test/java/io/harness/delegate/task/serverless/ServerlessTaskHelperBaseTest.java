/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.serverless;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryNgService;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.service.git.NGGitService;
import io.harness.connector.task.git.GitDecryptionHelper;
import io.harness.delegate.beans.connector.artifactoryconnector.*;
import io.harness.delegate.task.artifactory.ArtifactoryRequestMapper;
import io.harness.delegate.task.git.ScmFetchFilesHelperNG;
import io.harness.encryption.SecretRefData;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PIYUSH_BHUWALKA;
import static org.assertj.core.api.Assertions.assertThat;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

@OwnedBy(CDP)
public class ServerlessTaskHelperBaseTest extends CategoryTest{

    private static final String ARTIFACTORY_ARTIFACT_PATH = "artifactPath";
    private static final String ARTIFACTORY_ARTIFACT_NAME = "artifactName";
    private static final String ARTIFACT_FILE_NAME = "artifactFile";
    private static final String ARTIFACT_ZIP_REGEX = ".*\\.zip";
    private static final String ARTIFACT_JAR_REGEX = ".*\\.jar";
    private static final String ARTIFACT_WAR_REGEX = ".*\\.war";
    private static final String ARTIFACT_PATH_REGEX = ".*<\\+artifact\\.path>.*";
    private static final String ARTIFACT_PATH = "<+artifact.path>";

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock ServerlessGitFetchTaskHelper serverlessGitFetchTaskHelper;
    @Mock ScmFetchFilesHelperNG scmFetchFilesHelper;
    @Mock GitDecryptionHelper gitDecryptionHelper;
    @Mock NGGitService ngGitService;
    @Mock SecretDecryptionService secretDecryptionService;
    @Mock ArtifactoryNgService artifactoryNgService;
    @Mock ArtifactoryRequestMapper artifactoryRequestMapper;
    @InjectMocks @Spy ServerlessTaskHelperBase serverlessTaskHelperBase;

    private static final String ARTIFACT_DIRECTORY = "./artifact/serverless/";
    private static final String ARTIFACTORY_PATH = "asdffasd.zip";
    String manifestContent = "afsdf : <+artifact.path> ";
    String repositoryName = "dfsgvgasd";

    ArtifactoryConfigRequest artifactoryConfigRequest = ArtifactoryConfigRequest.builder().build();
    SecretRefData password = SecretRefData.builder().build();
    ArtifactoryAuthCredentialsDTO artifactoryAuthCredentialsDTO = ArtifactoryUsernamePasswordAuthDTO.builder().passwordRef(password).build();
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder().credentials(
            artifactoryAuthCredentialsDTO).authType(ArtifactoryAuthType.USER_PASSWORD).build();
    ArtifactoryConnectorDTO connectorConfigDTO = ArtifactoryConnectorDTO.builder().auth(artifactoryAuthenticationDTO).build();
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder().connectorConfig(connectorConfigDTO).build();
    List<EncryptedDataDetail> encryptedDataDetailsList = Arrays.asList(EncryptedDataDetail.builder().build());
    ServerlessArtifactConfig serverlessArtifactConfig = ServerlessArtifactoryArtifactConfig.builder().artifactPath(ARTIFACTORY_PATH).
            connectorDTO(connectorInfoDTO).encryptedDataDetails(encryptedDataDetailsList).repositoryName(repositoryName).build();
    @Mock LogCallback logCallback;
    ServerlessManifestConfig serverlessManifestConfig = ServerlessAwsLambdaManifestConfig.builder().manifestContent(manifestContent).build();

    @Test
    @Owner(developers = PIYUSH_BHUWALKA)
    @Category(UnitTests.class)
    public void fetchArtifactTest() throws Exception{

        String artifactoryDirectory = Paths.get(ARTIFACT_DIRECTORY, convertBase64UuidToCanonicalForm(generateUuid()))
                .normalize()
                .toAbsolutePath()
                .toString();
        doReturn(artifactoryConfigRequest).when(artifactoryRequestMapper).toArtifactoryRequest(connectorConfigDTO);
        Map<String, String> artifactMetadata = new HashMap<>();
        artifactMetadata.put(ARTIFACTORY_ARTIFACT_PATH, ARTIFACT_PATH);
        artifactMetadata.put(ARTIFACTORY_ARTIFACT_NAME, ARTIFACT_PATH);

        Optional<String> artifactFileFormat = Optional.of(".zip");
        String artifactFileName = ARTIFACT_FILE_NAME + artifactFileFormat.get();
        File artifactFile = new File(artifactoryDirectory + "/" + artifactFileName);

        doReturn("asdfsdfaewrq").when(serverlessTaskHelperBase).downloadArtifactoryArtifact((ServerlessArtifactoryArtifactConfig)
                eq(serverlessArtifactConfig), eq(logCallback), any());
        String input = "asfd";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        doReturn(inputStream).when(artifactoryNgService).downloadArtifacts(artifactoryConfigRequest, repositoryName, artifactMetadata,
                ARTIFACTORY_ARTIFACT_PATH, ARTIFACTORY_ARTIFACT_NAME);
        String path = serverlessTaskHelperBase.fetchArtifact(serverlessArtifactConfig, logCallback, ARTIFACT_DIRECTORY, serverlessManifestConfig);
        assertThat(path).isEqualTo("afsdf : asdfsdfaewrq ");
    }
}
