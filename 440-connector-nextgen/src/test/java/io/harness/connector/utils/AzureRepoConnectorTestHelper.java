package io.harness.connector.utils;

import static io.harness.delegate.beans.connector.scm.GitAuthType.HTTP;
import static io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessType.TOKEN;

import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoApiAccessDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoAuthenticationDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoConnectorDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpAuthenticationType;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoHttpCredentialsDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoTokenSpecDTO;
import io.harness.delegate.beans.connector.scm.azurerepo.AzureRepoUsernameTokenDTO;
import io.harness.encryption.SecretRefHelper;

public class AzureRepoConnectorTestHelper {
  public static AzureRepoConnectorDTO createConnectorDTO() {
    final String url = "url";
    final String tokenRef = "tokenRef";
    final String validationRepo = "validationRepo";

    final AzureRepoAuthenticationDTO azureRepoAuthenticationDTO =
        AzureRepoAuthenticationDTO.builder()
            .authType(HTTP)
            .credentials(
                AzureRepoHttpCredentialsDTO.builder()
                    .type(AzureRepoHttpAuthenticationType.USERNAME_AND_TOKEN)
                    .httpCredentialsSpec(
                        AzureRepoUsernameTokenDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
                    .build())
            .build();

    final AzureRepoApiAccessDTO azureRepoApiAccessDTO =
        AzureRepoApiAccessDTO.builder()
            .type(TOKEN)
            .spec(AzureRepoTokenSpecDTO.builder().tokenRef(SecretRefHelper.createSecretRef(tokenRef)).build())
            .build();

    return AzureRepoConnectorDTO.builder()
        .url(url)
        .validationRepo(validationRepo)
        .connectionType(GitConnectionType.ACCOUNT)
        .authentication(azureRepoAuthenticationDTO)
        .apiAccess(azureRepoApiAccessDTO)
        .build();
  }
}
