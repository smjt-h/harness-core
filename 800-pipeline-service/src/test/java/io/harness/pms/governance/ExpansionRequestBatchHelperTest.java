package io.harness.pms.governance;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class ExpansionRequestBatchHelperTest extends CategoryTest {
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