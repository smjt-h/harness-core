/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cf.client.api.BaseConfig;
import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.connector.HarnessConfig;
import io.harness.cf.client.connector.HarnessConnector;
import io.harness.cf.openapi.ApiClient;
import io.harness.cf.client.dto.Target;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CfClientModule extends AbstractModule {
  private static volatile CfClientModule instance;
  private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  public static CfClientModule getInstance() {
    if (instance == null) {
      instance = new CfClientModule();
    }
    return instance;
  }

  private CfClientModule() {}

  @Provides
  @Singleton
  CfClient provideCfClient(CfClientConfig cfClientConfig) {
    log.info("Using CF API key {}", cfClientConfig.getApiKey());
    String apiKey = cfClientConfig.getApiKey();
    if (isEmpty(apiKey)) {
      apiKey = "fake";
    }

    HarnessConfig harnessConfig = HarnessConfig.builder()
                                      .configUrl(cfClientConfig.getConfigUrl())
                                      .eventUrl(cfClientConfig.getEventUrl())
                                      .connectionTimeout(cfClientConfig.getConnectionTimeout())
                                      .readTimeout(cfClientConfig.getReadTimeout())
                                      .build();

    final HarnessConnector harnessConnector = new HarnessConnector(cfClientConfig.getApiKey(), harnessConfig);

    final BaseConfig config = BaseConfig.builder().bufferSize(5).analyticsEnabled(cfClientConfig.isAnalyticsEnabled()).build();

    final CfClient client = new CfClient(harnessConnector, config);
    try {
      client.waitForInitialization();
      final Target target =
              Target.builder()
                      .identifier("target1")
                      .isPrivate(false)
                      .attribute("testKey", "TestValue")
                      .name("target1")
                      .build();
      scheduler.scheduleAtFixedRate(
              () -> {
                client.boolVariation("DavesNewFlag", target, false);
              }, 0,
              2,
              TimeUnit.SECONDS);

    } catch (Exception e) {
      return null;
    }

    return client;
  }

  @Provides
  @Singleton
  @Named("cfMigrationAPI")
  CFApi providesCfAPI(CfMigrationConfig migrationConfig) {
    ApiClient apiClient = new ApiClient();
    apiClient.setReadTimeout(migrationConfig.getReadTimeout());
    apiClient.setConnectTimeout(migrationConfig.getConnectionTimeout());
    apiClient.setBasePath(migrationConfig.getAdminUrl());
    return new CFApi(apiClient);
  }
}