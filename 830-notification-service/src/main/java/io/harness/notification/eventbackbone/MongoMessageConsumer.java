/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.notification.eventbackbone;

import static io.harness.AuthorizationServiceHeader.NOTIFICATION_SERVICE;

import io.harness.NotificationRequest;
import io.harness.notification.entities.MongoNotificationRequest;
import io.harness.notification.service.api.NotificationService;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListener;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.dto.ServicePrincipal;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MongoMessageConsumer extends QueueListener<MongoNotificationRequest> implements MessageConsumer {
  private NotificationService notificationService;
  @Inject private QueueController queueController;

  @Inject
  public MongoMessageConsumer(
      QueueConsumer<MongoNotificationRequest> queueConsumer, NotificationService notificationService) {
    super(queueConsumer, true);
    this.notificationService = notificationService;
  }

  @Override
  public void onMessage(MongoNotificationRequest message) {
    try {
      SecurityContextBuilder.setContext(new ServicePrincipal(NOTIFICATION_SERVICE.getServiceId()));
      while (!Thread.currentThread().isInterrupted()) {
        if (queueController.isNotPrimary()) {
          log.info("EntityActivity consumer is not running on primary deployment, will try again after some time...");
          TimeUnit.SECONDS.sleep(30);
          continue;
        }
        processNewMessage(message);
      }
    } catch (InterruptedException ex) {
      SecurityContextBuilder.unsetCompleteContext();
      Thread.currentThread().interrupt();
    } catch (InvalidProtocolBufferException e) {
      log.error("Corrupted message received off the mongo queue");
    } finally {
      SecurityContextBuilder.unsetCompleteContext();
    }
  }

  private void processNewMessage(MongoNotificationRequest message) throws InvalidProtocolBufferException {
    NotificationRequest notificationRequest = NotificationRequest.parseFrom(message.getBytes());
    if (!notificationRequest.getUnknownFields().asMap().isEmpty()) {
      throw new InvalidProtocolBufferException("Unknown fields detected. Check Notification Request producer");
    }
    notificationService.processNewMessage(notificationRequest);
  }
}
