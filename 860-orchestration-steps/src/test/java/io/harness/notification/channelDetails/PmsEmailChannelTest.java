/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.notification.channelDetails;

import static io.harness.rule.OwnerRule.BRIJESH;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.channeldetails.NotificationChannel;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsEmailChannelTest extends CategoryTest {
  List<String> userGroups = new ArrayList<>();
  List<String> recipients = Collections.singletonList("reciepient@gmail.com");
  PmsEmailChannel pmsEmailChannel;
  @Before
  public void setUp() {
    userGroups.add("user");
    userGroups.add("org.user");
    pmsEmailChannel = spy(new PmsEmailChannel(userGroups, recipients));
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testToNotificationChannel() {
    String accountId = "accountId";
    String orgId = "orgId";
    String projectId = "projectId";
    String templateId = "temolateId";
    Map<String, String> templateData = new HashMap<>();
    NotificationChannel notificationChannel =
        pmsEmailChannel.toNotificationChannel(accountId, orgId, projectId, templateId, templateData);
    assertEquals(notificationChannel.getAccountId(), accountId);
    assertEquals(((EmailChannel) notificationChannel).getRecipients(), recipients);
    assertEquals(notificationChannel.getUserGroups().get(0).getOrgIdentifier(), orgId);
    assertEquals(notificationChannel.getUserGroups().get(0).getProjectIdentifier(), projectId);

    assertEquals(notificationChannel.getUserGroups().get(1).getOrgIdentifier(), orgId);
    assertEquals(notificationChannel.getUserGroups().get(1).getProjectIdentifier(), "");
  }
}
