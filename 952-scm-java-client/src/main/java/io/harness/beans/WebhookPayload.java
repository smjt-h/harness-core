/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class WebhookPayload {
  private WebhookGitUser webhookGitUser;
  private Repository repository;
  private WebhookEvent webhookEvent;
  private ParseWebhookResponse parseWebhookResponse;
}
