/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.LogCallback;

import software.wings.service.impl.AwsApiHelperService;
import software.wings.service.impl.aws.client.CloseableAmazonWebServiceClient;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.amazonaws.services.cloudformation.model.UpdateStackRequest;
import com.amazonaws.services.cloudformation.model.UpdateStackResult;
import com.amazonaws.services.cloudformation.waiters.AmazonCloudFormationWaiters;
import com.amazonaws.waiters.WaiterHandler;
import com.amazonaws.waiters.WaiterParameters;
import com.amazonaws.waiters.WaiterTimedOutException;
import com.amazonaws.waiters.WaiterUnrecoverableException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Singleton
@Slf4j

public class AWSCloudformationClientImpl implements AWSCloudformationClient {
  private static final String S3STORE = "s3";
  @Inject AwsApiHelperService awsApiHelperService;
  @Inject AwsCloudformationPrintHelper awsCloudformationPrintHelper;
  @Inject private AwsCallTracker tracker;

  @Override
  public List<Stack> getAllStacks(
      String region, DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      List<Stack> stacks = new ArrayList<>();
      String nextToken = null;
      do {
        describeStacksRequest.withNextToken(nextToken);
        tracker.trackCFCall("Describe Stacks");
        DescribeStacksResult result =
            closeableAmazonCloudFormationClient.getClient().describeStacks(describeStacksRequest);
        nextToken = result.getNextToken();
        stacks.addAll(result.getStacks());
      } while (nextToken != null);
      return stacks;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getAllStacks", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }

  @Override
  public void deleteStack(String region, DeleteStackRequest deleteStackRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      String msg = "# Starting to delete stack:" + deleteStackRequest.getStackName();
      tracker.trackCFCall(msg);
      closeableAmazonCloudFormationClient.getClient().deleteStack(deleteStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception deleteStack", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<StackResource> getAllStackResources(
      String region, DescribeStackResourcesRequest describeStackResourcesRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      tracker.trackCFCall("Describe Stack Resources");
      DescribeStackResourcesResult result =
          closeableAmazonCloudFormationClient.getClient().describeStackResources(describeStackResourcesRequest);
      return result.getStackResources();
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception retrieving StackResources", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }

  @Override
  public List<StackEvent> getAllStackEvents(String region, DescribeStackEventsRequest describeStackEventsRequest,
      AwsInternalConfig awsConfig, long lastStackEventsTs) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      List<StackEvent> stacksEvents = new ArrayList<>();
      String nextToken = null;
      boolean oldStackEventExists;
      do {
        describeStackEventsRequest.withNextToken(nextToken);
        tracker.trackCFCall("Describe Stack Events");
        DescribeStackEventsResult result =
            closeableAmazonCloudFormationClient.getClient().describeStackEvents(describeStackEventsRequest);
        nextToken = result.getNextToken();
        stacksEvents.addAll(result.getStackEvents());

        oldStackEventExists =
            result.getStackEvents().stream().anyMatch(event -> event.getTimestamp().getTime() < lastStackEventsTs);
        nextToken = oldStackEventExists ? null : nextToken;

      } while (nextToken != null);
      return stacksEvents;
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getAllStackEvents", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }

  @Override
  public CreateStackResult createStack(
      String region, CreateStackRequest createStackRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      tracker.trackCFCall("Create Stack");
      return closeableAmazonCloudFormationClient.getClient().createStack(createStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception createStack", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return new CreateStackResult();
  }

  @Override
  public UpdateStackResult updateStack(
      String region, UpdateStackRequest updateStackRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      tracker.trackCFCall("Update Stack");
      return closeableAmazonCloudFormationClient.getClient().updateStack(updateStackRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception updateStack", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return new UpdateStackResult();
  }

  @Override
  public DescribeStacksResult describeStacks(
      String region, DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      tracker.trackCFCall("Describe Stacks");
      return closeableAmazonCloudFormationClient.getClient().describeStacks(describeStacksRequest);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception describeStacks", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return new DescribeStacksResult();
  }

  @Override
  public void waitForStackDeletionCompleted(DescribeStacksRequest describeStacksRequest, AwsInternalConfig awsConfig,
      String region, LogCallback logCallback, long stackEventsTs) {
    long lastStackEventsTs = stackEventsTs;
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      AmazonCloudFormationWaiters waiter = getAmazonCloudFormationWaiter(closeableAmazonCloudFormationClient);
      WaiterParameters<DescribeStacksRequest> parameters = new WaiterParameters<>(describeStacksRequest);
      parameters = parameters.withRequest(describeStacksRequest);
      Future future = waiter.stackDeleteComplete().runAsync(parameters, new WaiterHandler() {
        @Override
        public void onWaitSuccess(AmazonWebServiceRequest amazonWebServiceRequest) {
          logCallback.saveExecutionLog("Stack deletion completed");
        }
        @Override
        public void onWaitFailure(Exception e) {
          logCallback.saveExecutionLog(format("Stack deletion failed: %s", e.getMessage()));
        }
      });
      while (!future.isDone()) {
        sleep(ofSeconds(10));
        List<StackEvent> stackEvents = getAllStackEvents(region,
            new DescribeStackEventsRequest().withStackName(describeStacksRequest.getStackName()), awsConfig,
            lastStackEventsTs);
        lastStackEventsTs = awsCloudformationPrintHelper.printStackEvents(stackEvents, lastStackEventsTs, logCallback);
      }
      future.get();
      List<StackResource> stackResources = getAllStackResources(
          region, new DescribeStackResourcesRequest().withStackName(describeStacksRequest.getStackName()), awsConfig);
      awsCloudformationPrintHelper.printStackResources(stackResources, logCallback);
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (WaiterUnrecoverableException | WaiterTimedOutException waiterUnrecoverableException) {
      throw new InvalidRequestException(
          ExceptionUtils.getMessage(waiterUnrecoverableException), waiterUnrecoverableException);
    } catch (Exception e) {
      log.error("Exception deleting stack ", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public List<ParameterDeclaration> getParamsData(
      AwsInternalConfig awsConfig, String region, String data, AwsCFTemplatesType awsCFTemplatesType) {
    try (CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient =
             new CloseableAmazonWebServiceClient(getAmazonCloudFormationClient(Regions.fromName(region), awsConfig))) {
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      if (AwsCFTemplatesType.S3 == awsCFTemplatesType) {
        request.withTemplateURL(normalizeS3TemplatePath(data));
      } else {
        request.withTemplateBody(data);
      }
      tracker.trackCFCall("Get Template Summary");
      GetTemplateSummaryResult result = closeableAmazonCloudFormationClient.getClient().getTemplateSummary(request);
      List<ParameterDeclaration> parameters = result.getParameters();
      if (isNotEmpty(parameters)) {
        return parameters;
      }
    } catch (AmazonServiceException amazonServiceException) {
      awsApiHelperService.handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      awsApiHelperService.handleAmazonClientException(amazonClientException);
    } catch (Exception e) {
      log.error("Exception getParamsData", e);
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
    return emptyList();
  }

  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(Regions region, AwsInternalConfig awsConfig) {
    AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard().withRegion(region);
    awsApiHelperService.attachCredentialsAndBackoffPolicy(builder, awsConfig);
    return (AmazonCloudFormationClient) builder.build();
  }

  @VisibleForTesting
  AmazonCloudFormationWaiters getAmazonCloudFormationWaiter(
      CloseableAmazonWebServiceClient<AmazonCloudFormationClient> closeableAmazonCloudFormationClient) {
    return new AmazonCloudFormationWaiters(closeableAmazonCloudFormationClient.getClient());
  }

  private String normalizeS3TemplatePath(String s3Path) {
    String normalizedS3TemplatePath = s3Path;
    if (isNotEmpty(normalizedS3TemplatePath) && normalizedS3TemplatePath.contains("+")) {
      normalizedS3TemplatePath = s3Path.replaceAll("\\+", "%20");
    }
    return normalizedS3TemplatePath;
  }
}
