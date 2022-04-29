/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.envgroup;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.envgroup.remote.EnvironmentGroupResourceClient;
import io.harness.envgroup.remote.EnvironmentGroupResourceClientHttpFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

@OwnedBy(PIPELINE)
public class EnvironmentGroupResourceClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;

  @Inject
  public EnvironmentGroupResourceClientModule(
      ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
  }

  @Provides
  private EnvironmentGroupResourceClientHttpFactory secretManagerHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new EnvironmentGroupResourceClientHttpFactory(
        this.ngManagerClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Override
  protected void configure() {
    this.bind(EnvironmentGroupResourceClient.class)
        .toProvider(EnvironmentGroupResourceClientHttpFactory.class)
        .in(Scopes.SINGLETON);
  }
}
