package io.harness.debezium;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoOffsetBackingStoreTest extends DebeziumTestBase {
  Map<ByteBuffer, ByteBuffer> data = new HashMap<>();
  @InjectMocks MongoOffsetBackingStore mongoOffsetBackingStore;
  @Inject MongoTemplate mongoTemplate;
  private final String collectionName = "debeziumOffset";

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testLoad() {
    MockitoAnnotations.initMocks(this);
    mongoOffsetBackingStore.setCollectionName(collectionName);
    mongoOffsetBackingStore.setData(data);
    mongoOffsetBackingStore.setMongoTemplate(mongoTemplate);
    mongoOffsetBackingStore.load();
    assertThat(mongoOffsetBackingStore.isFoundOffsets()).isEqualTo(false);
    byte[] keyBytes = {23, 24, 56};
    byte[] valueBytes = {12, 20, 60};
    ByteBuffer key = ByteBuffer.wrap(keyBytes);
    ByteBuffer value = ByteBuffer.wrap(valueBytes);
    data.put(key, value);
    mongoOffsetBackingStore.setData(data);
    mongoOffsetBackingStore.save();
    mongoOffsetBackingStore.load();
    assertThat(mongoOffsetBackingStore.isFoundOffsets()).isEqualTo(true);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testSave() {
    MockitoAnnotations.initMocks(this);
    mongoOffsetBackingStore.setCollectionName(collectionName);
    mongoOffsetBackingStore.setData(data);
    mongoOffsetBackingStore.setMongoTemplate(mongoTemplate);
    mongoOffsetBackingStore.save();
    assertThat(mongoOffsetBackingStore.isSavedOffset()).isEqualTo(false);
    byte[] keyBytes = {23, 24, 56};
    byte[] valueBytes = {12, 20, 60};
    ByteBuffer key = ByteBuffer.wrap(keyBytes);
    ByteBuffer value = ByteBuffer.wrap(valueBytes);
    data.put(key, value);
    mongoOffsetBackingStore.setData(data);
    mongoOffsetBackingStore.save();
    assertThat(mongoOffsetBackingStore.isSavedOffset()).isEqualTo(true);
  }
}
