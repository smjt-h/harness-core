/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionRequestMetadata;
import io.harness.pms.contracts.governance.ExpansionRequestProto;
import io.harness.pms.contracts.governance.ExpansionResponseBatch;
import io.harness.pms.contracts.governance.ExpansionResponseProto;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceBlockingStub;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceImplBase;
import io.harness.rule.Owner;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(PIPELINE)
public class JsonExpanderTest extends CategoryTest {
  @InjectMocks JsonExpander jsonExpander;
  JsonExpansionServiceBlockingStub blockingStub;
  @Rule public GrpcCleanupRule grpcCleanup;
  JsonExpansionServiceImplBase jsonExpansionServiceImplBase;

  @Before
  public void setUp() {
    grpcCleanup = new GrpcCleanupRule();

    jsonExpansionServiceImplBase = new JsonExpansionServiceImplBase() {
      @Override
      public void expand(ExpansionRequestBatch request, StreamObserver<ExpansionResponseBatch> responseObserver) {
        responseObserver.onNext(ExpansionResponseBatch.newBuilder()
                                    .addExpansionResponseProto(ExpansionResponseProto.newBuilder()
                                                                   .setUuid("fqn/connectorRef")
                                                                   .setKey("proofThatItIsFromHere")
                                                                   .setSuccess(true)
                                                                   .build())
                                    .build());
        responseObserver.onCompleted();
      }
    };
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testFetchExpansionResponses() throws IOException {
    grpcCleanup.register(InProcessServerBuilder.forName("expander")
                             .directExecutor()
                             .addService(jsonExpansionServiceImplBase)
                             .build()
                             .start());
    ManagedChannel channel = grpcCleanup.register(InProcessChannelBuilder.forName("expander").directExecutor().build());
    blockingStub = JsonExpansionServiceGrpc.newBlockingStub(channel);
    on(jsonExpander).set("jsonExpansionServiceBlockingStubMap", Collections.singletonMap(ModuleType.PMS, blockingStub));
    on(jsonExpander).set("executor", Executors.newFixedThreadPool(5));

    Set<ExpansionResponseBatch> empty = jsonExpander.fetchExpansionResponses(Collections.emptyMap());
    assertThat(empty).isEmpty();
    ExpansionRequestProto expansionRequest = ExpansionRequestProto.newBuilder()
                                                 .setUuid("fqn/connectorRef")
                                                 .setKey("connectorRef")
                                                 .setValue(ByteString.copyFromUtf8("k8sConn"))
                                                 .build();
    ExpansionRequestBatch expansionRequestBatch = ExpansionRequestBatch.newBuilder()
                                                      .addExpansionRequestProto(expansionRequest)
                                                      .setRequestMetadata(ExpansionRequestMetadata.getDefaultInstance())
                                                      .build();
    Set<ExpansionResponseBatch> oneBatch =
        jsonExpander.fetchExpansionResponses(Collections.singletonMap(ModuleType.PMS, expansionRequestBatch));
    assertThat(oneBatch).hasSize(1);
    ExpansionResponseBatch responseBatch = new ArrayList<>(oneBatch).get(0);
    List<ExpansionResponseProto> batchList = responseBatch.getExpansionResponseProtoList();
    assertThat(batchList).hasSize(1);
    ExpansionResponseProto response = batchList.get(0);
    assertThat(response.getSuccess()).isTrue();
    assertThat(response.getKey()).isEqualTo("proofThatItIsFromHere");
    assertThat(response.getUuid()).isEqualTo("fqn/connectorRef");
  }
}
