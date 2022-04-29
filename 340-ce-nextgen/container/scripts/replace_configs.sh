#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

CONFIG_FILE=/opt/harness/config.yml

replace_key_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq write -i $CONFIG_FILE $CONFIG_KEY $CONFIG_VALUE
  fi
}

#
yq delete -i $CONFIG_FILE server.adminConnectors
yq delete -i $CONFIG_FILE server.applicationConnectors[0]

replace_key_value logging.level $LOGGING_LEVEL

replace_key_value server.applicationConnectors[0].port $CE_NEXTGEN_PORT

replace_key_value events-mongo.uri "${EVENTS_MONGO_DB_URL//\\&/&}"
replace_key_value events-mongo.indexManagerMode $EVENTS_MONGO_INDEX_MANAGER_MODE

replace_key_value ngManagerClientConfig.baseUrl $NG_MANAGER_CLIENT_BASEURL
replace_key_value managerClientConfig.baseUrl $MANAGER_CLIENT_BASEURL

replace_key_value ngManagerServiceSecret $NEXT_GEN_MANAGER_SECRET
replace_key_value jwtAuthSecret $JWT_AUTH_SECRET
replace_key_value jwtIdentityServiceSecret $JWT_IDENTITY_SERVICE_SECRET

replace_key_value timescaledb.timescaledbUrl "$TIMESCALEDB_URI"
replace_key_value timescaledb.timescaledbUsername "$TIMESCALEDB_USERNAME"
replace_key_value timescaledb.timescaledbPassword "$TIMESCALEDB_PASSWORD"

replace_key_value eventsFramework.redis.sentinel $EVENTS_FRAMEWORK_USE_SENTINEL
replace_key_value eventsFramework.redis.envNamespace $EVENTS_FRAMEWORK_ENV_NAMESPACE
replace_key_value eventsFramework.redis.redisUrl $EVENTS_FRAMEWORK_REDIS_URL
replace_key_value eventsFramework.redis.masterName $EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME
replace_key_value eventsFramework.redis.userName $EVENTS_FRAMEWORK_REDIS_USERNAME
replace_key_value eventsFramework.redis.password $EVENTS_FRAMEWORK_REDIS_PASSWORD
replace_key_value eventsFramework.redis.sslConfig.enabled $EVENTS_FRAMEWORK_REDIS_SSL_ENABLED
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePath $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePassword $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD

replace_key_value gcpConfig.gcpProjectId "$GCP_PROJECT_ID"
replace_key_value gcpConfig.gcpAwsConnectorCrudPubSubTopic "$GCP_AWS_CONNECTOR_CRUD_PUBSUB_TOPIC"

replace_key_value ceAzureSetupConfig.azureAppClientId "$AZURE_APP_CLIENT_ID"
replace_key_value ceAzureSetupConfig.azureAppClientSecret "$AZURE_APP_CLIENT_SECRET"
replace_key_value ceAzureSetupConfig.enableFileCheckAtSource "$AZURE_ENABLE_FILE_CHECK_AT_SOURCE"

replace_key_value awsConfig.accessKey "$AWS_ACCESS_KEY"
replace_key_value awsConfig.secretKey "$AWS_SECRET_KEY"
replace_key_value awsConfig.destinationBucket "$AWS_DESTINATION_BUCKET"
replace_key_value awsConfig.harnessAwsAccountId "$AWS_ACCOUNT_ID"
replace_key_value awsConfig.awsConnectorTemplate "$AWS_TEMPLATE_LINK"
replace_key_value cfClientConfig.apiKey "$CF_CLIENT_API_KEY"
replace_key_value cfClientConfig.configUrl "$CF_CLIENT_CONFIG_URL"
replace_key_value cfClientConfig.eventUrl "$CF_CLIENT_EVENT_URL"
replace_key_value cfClientConfig.analyticsEnabled "$CF_CLIENT_ANALYTICS_ENABLED"
replace_key_value cfClientConfig.connectionTimeout "$CF_CLIENT_CONNECTION_TIMEOUT"
replace_key_value cfClientConfig.readTimeout "$CF_CLIENT_READ_TIMEOUT"
replace_key_value featureFlagConfig.featureFlagSystem "$FEATURE_FLAG_SYSTEM"
replace_key_value featureFlagConfig.syncFeaturesToCF "$SYNC_FEATURES_TO_CF"

replace_key_value grpcClient.target "$MANAGER_TARGET"
replace_key_value grpcClient.authority "$MANAGER_AUTHORITY"

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq write -i $CONFIG_FILE eventsFramework.redis.sentinelUrls.[$INDEX] "${REDIS_SENTINEL_URL}"
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq delete -i $CONFIG_FILE logging.appenders[0]
  yq write -i $CONFIG_FILE logging.appenders[0].stackdriverLogEnabled "true"
else
  yq delete -i $CONFIG_FILE logging.appenders[1]
fi
