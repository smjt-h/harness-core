package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.governance.ExpansionRequestBatch;
import io.harness.pms.contracts.governance.ExpansionRequestMetadata;
import io.harness.pms.contracts.governance.ExpansionRequestProto;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ExpansionRequestBatchHelperTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetBatchesByModule() {
    ExpansionRequest cdK8sConn1 = ExpansionRequest.builder()
                                      .fqn("fqn1")
                                      .module(ModuleType.CD)
                                      .key("connectorRef")
                                      .fieldValue(new TextNode("k8sConn"))
                                      .build();
    ExpansionRequest cdK8sConn2 = ExpansionRequest.builder()
                                      .fqn("fqn2")
                                      .module(ModuleType.CD)
                                      .key("connectorRef")
                                      .fieldValue(new TextNode("k8sConn"))
                                      .build();
    ExpansionRequest cdDockerConn = ExpansionRequest.builder()
                                        .fqn("fqn3")
                                        .module(ModuleType.CD)
                                        .key("connectorRef")
                                        .fieldValue(new TextNode("dockerConn"))
                                        .build();
    ExpansionRequest ciK8sConn1 = ExpansionRequest.builder()
                                      .fqn("fqn4")
                                      .module(ModuleType.CI)
                                      .key("connectorRef")
                                      .fieldValue(new TextNode("k8sConn"))
                                      .build();
    ExpansionRequest ciK8sConn2 = ExpansionRequest.builder()
                                      .fqn("fqn5")
                                      .module(ModuleType.CI)
                                      .key("connectorRef")
                                      .fieldValue(new TextNode("k8sConn"))
                                      .build();
    Set<ExpansionRequest> expansionRequests =
        Stream.of(cdK8sConn1, cdK8sConn2, cdDockerConn, ciK8sConn1, ciK8sConn2).collect(Collectors.toSet());
    ExpansionRequestMetadata metadata =
        ExpansionRequestMetadata.newBuilder().setAccountId("acc").setOrgId("org").setProjectId("proj").build();
    ExpansionRequestData batchesByModule = ExpansionRequestBatchHelper.getBatchesByModule(expansionRequests, metadata);
    Map<ModuleType, ExpansionRequestBatch> moduleToRequestBatch = batchesByModule.getModuleToRequestBatch();
    assertThat(moduleToRequestBatch).hasSize(2);
    List<ExpansionRequestProto> cdRequests = moduleToRequestBatch.get(ModuleType.CD).getExpansionRequestProtoList();
    assertThat(cdRequests).hasSize(2);
    List<ExpansionRequestProto> ciRequests = moduleToRequestBatch.get(ModuleType.CI).getExpansionRequestProtoList();
    assertThat(ciRequests).hasSize(1);
    Map<String, Set<String>> uuidToFqnSet = batchesByModule.getUuidToFqnSet();
    assertThat(uuidToFqnSet).hasSize(3);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testConvertToByteString() throws IOException {
    String yaml = "pipeline:\n"
        + "  identifier: s1\n"
        + "  tags:\n"
        + "    a: b\n"
        + "    c: d\n"
        + "  list:\n"
        + "    - l1\n"
        + "    - l2";
    YamlField yamlField = YamlUtils.readTree(yaml);
    YamlNode pipelineNode = yamlField.getNode().getFieldOrThrow("pipeline").getNode();
    JsonNode idNode = pipelineNode.getFieldOrThrow("identifier").getNode().getCurrJsonNode();
    JsonNode tagsNode = pipelineNode.getFieldOrThrow("tags").getNode().getCurrJsonNode();
    JsonNode listNode = pipelineNode.getFieldOrThrow("list").getNode().getCurrJsonNode();
    ByteString idBytes = ExpansionRequestBatchHelper.convertToByteString(idNode);
    ByteString tagBytes = ExpansionRequestBatchHelper.convertToByteString(tagsNode);
    ByteString listBytes = ExpansionRequestBatchHelper.convertToByteString(listNode);
    assertThat(idBytes).isNotNull();
    assertThat(tagBytes).isNotNull();
    assertThat(listBytes).isNotNull();
    assertThat(YamlUtils.readTree(idBytes.toStringUtf8()).getNode().getCurrJsonNode()).isEqualTo(idNode);
    assertThat(YamlUtils.readTree(tagBytes.toStringUtf8()).getNode().getCurrJsonNode()).isEqualTo(tagsNode);
    assertThat(YamlUtils.readTree(listBytes.toStringUtf8()).getNode().getCurrJsonNode()).isEqualTo(listNode);
  }
}