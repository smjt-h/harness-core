package io.harness.debezium;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.rule.OwnerRule.SHALINI;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
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
  private byte[] keyBytes = {24, 78};
  private byte[] valueBytes = {102, 98};

  public void initialize() {
    MockitoAnnotations.initMocks(this);
    mongoOffsetBackingStore.setMongoTemplate(mongoTemplate);
    mongoOffsetBackingStore.setCollectionName(collectionName);
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testLoad() {
    initialize();
    mongoOffsetBackingStore.load();
    assertEquals(isEmpty(mongoOffsetBackingStore.getData()), true);
    mongoTemplate.save(
        DebeziumOffset.builder().key(keyBytes).value(valueBytes).createdAt(System.currentTimeMillis()).build(),
        collectionName);
    mongoOffsetBackingStore.load();
    assertEquals(!isEmpty(mongoOffsetBackingStore.getData()), true);
  }

  @Test
  @Owner(developers = SHALINI)
  @Category(UnitTests.class)
  public void testSave() {
    initialize();
    ByteBuffer key = ByteBuffer.wrap(keyBytes);
    ByteBuffer value = ByteBuffer.wrap(valueBytes);
    data.put(key, value);
    mongoOffsetBackingStore.setData(data);
    mongoOffsetBackingStore.save();
    List<DebeziumOffset> debeziumOffset = mongoTemplate.findAll(DebeziumOffset.class, collectionName);
    assertNotNull(debeziumOffset);
  }
}
