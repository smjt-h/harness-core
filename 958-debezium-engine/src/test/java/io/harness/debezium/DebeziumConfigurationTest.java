package io.harness.debezium;

import static junit.framework.TestCase.assertEquals;

import io.harness.category.element.UnitTests;

import java.util.Optional;
import java.util.Properties;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DebeziumConfigurationTest {
  private static final String MONGO_DB_CONNECTOR = "io.debezium.connector.mongodb.MongoDbConnector";
  private static final String CONNECTOR_NAME = "name";
  private static final String OFFSET_STORAGE = "offset.storage";
  private static final String OFFSET_STORAGE_FILE_FILENAME = "offset.storage.file.filename";
  private static final String OFFSET_STORAGE_COLLECTION = "offset.storage.topic";
  private static final String KEY_CONVERTER_SCHEMAS_ENABLE = "key.converter.schemas.enable";
  private static final String VALUE_CONVERTER_SCHEMAS_ENABLE = "value.converter.schemas.enable";
  private static final String OFFSET_FLUSH_INTERVAL_MS = "offset.flush.interval.ms";
  private static final String CONNECTOR_CLASS = "connector.class";
  private static final String MONGODB_HOSTS = "mongodb.hosts";
  private static final String MONGODB_NAME = "mongodb.name";
  private static final String MONGODB_USER = "mongodb.user";
  private static final String MONGODB_PASSWORD = "mongodb.password";
  private static final String MONGODB_SSL_ENABLED = "mongodb.ssl.enabled";
  private static final String DATABASE_INCLUDE_LIST = "database.include.list";
  private static final String COLLECTION_INCLUDE_LIST = "collection.include.list";
  private static final String TRANSFORMS = "transforms";
  private static final String TRANSFORMS_UNWRAP_TYPE = "transforms.unwrap.type";
  private static final String TRANSFORMS_UNWRAP_DROP_TOMBSTONES = "transforms.unwrap.drop.tombstones";
  private static final String TRANSFORMS_UNWRAP_ADD_HEADERS = "transforms.unwrap.add.headers";
  private static final String DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE =
      "io.debezium.connector.mongodb.transforms.ExtractNewDocumentState";

  @Test
  @Category(UnitTests.class)
  public void testGetDebeziumProperties() {
    DebeziumConfig debeziumConfig = new DebeziumConfig(false, "testConnector", "offset_file", "false", "false", "6000",
        "MongoDbConnectorClass", "rs0/host1", "shop", "", "", "false", "products", "");
    Properties expected_props = new Properties();
    Properties props = new DebeziumConfiguration().getDebeziumProperties(debeziumConfig);
    expected_props.setProperty(CONNECTOR_NAME, debeziumConfig.getConnectorName());
    expected_props.setProperty(OFFSET_STORAGE, MongoOffsetBackingStore.class.getName());
    expected_props.setProperty(OFFSET_STORAGE_FILE_FILENAME, debeziumConfig.getOffsetStorageFileName());
    expected_props.setProperty(OFFSET_STORAGE_COLLECTION, DebeziumOffset.OFFSET_COLLECTION);
    expected_props.setProperty(KEY_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getKeyConverterSchemasEnable());
    expected_props.setProperty(VALUE_CONVERTER_SCHEMAS_ENABLE, debeziumConfig.getValueConverterSchemasEnable());
    expected_props.setProperty(OFFSET_FLUSH_INTERVAL_MS, debeziumConfig.getOffsetFlushIntervalMillis());

    /* begin connector properties */
    expected_props.setProperty(CONNECTOR_CLASS, MONGO_DB_CONNECTOR);
    expected_props.setProperty(MONGODB_HOSTS, debeziumConfig.getMongodbHosts());
    expected_props.setProperty(MONGODB_NAME, debeziumConfig.getMongodbName());
    Optional.ofNullable(debeziumConfig.getMongodbUser())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_USER, x));
    Optional.ofNullable(debeziumConfig.getMongodbPassword())
        .filter(x -> !x.isEmpty())
        .ifPresent(x -> props.setProperty(MONGODB_PASSWORD, x));
    expected_props.setProperty(MONGODB_SSL_ENABLED, debeziumConfig.getSslEnabled());
    expected_props.setProperty(DATABASE_INCLUDE_LIST, debeziumConfig.getDatabaseIncludeList());
    expected_props.setProperty(COLLECTION_INCLUDE_LIST, debeziumConfig.getCollectionIncludeList());
    expected_props.setProperty(TRANSFORMS, "unwrap");
    expected_props.setProperty(
        TRANSFORMS_UNWRAP_TYPE, DEBEZIUM_CONNECTOR_MONGODB_TRANSFORMS_EXTRACT_NEW_DOCUMENT_STATE);
    expected_props.setProperty(TRANSFORMS_UNWRAP_DROP_TOMBSTONES, "false");
    expected_props.setProperty(TRANSFORMS_UNWRAP_ADD_HEADERS, "op");
    assertEquals(expected_props, props);
  }
}