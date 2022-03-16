/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface ConnectorTestConstants {
  String ACCOUNT_IDENTIFIER = "accountIdentifier";
  String ORG_IDENTIFIER = "orgIdentifier";
  String PROJECT_IDENTIFIER = "projectIdentifier";
  String CONNECTOR_NAME = "connectorName";
  String CONNECTOR_IDENTIFIER = "connectorIdentifier";
  String SECRET_IDENTIFIER = "secretIdentifier";
  String SSK_KEY_REF_IDENTIFIER = "sskKeyRefIdentifier";
  String SSK_KEY_REF_IDENTIFIER_WITH_ACCOUNT_SCOPE = "account.sskKeyRefIdentifier";

  String HOST = "1.1.1.1";
  String HOST_WITH_PORT = "1.1.1.1:8080";
  String HOST_NAME_1 = "hostName1";
  String HOST_NAME_2 = "hostName2";
  String ATTRIBUTE_TYPE_1 = "attributeType1";
  String ATTRIBUTE_NAME_1 = "attributeName1";
  String ATTRIBUTE_TYPE_2 = "attributeType2";
  String ATTRIBUTE_NAME_2 = "attributeName2";
}
