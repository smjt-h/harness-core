package io.harness.delegate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.connector.ConnectorType.DOCKER;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.rule.Owner;
import io.harness.serializer.ClassResolver;
import io.harness.serializer.HKryo;
import io.harness.serializer.OrdinalBackwardEnumNameSerializer;

import software.wings.WingsBaseTest;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.DEL)
public class KryoEnumSerializationTest extends WingsBaseTest {
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testWriteOrdinalReadName() {
    Kryo kryo = new Kryo(new ClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
    kryo.register(ConnectorInfoDTO.class, 500);
    kryo.register(ConnectorType.class, 501);
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(DOCKER)
                                            .description(generateUuid())
                                            .identifier(generateUuid())
                                            .name(generateUuid())
                                            .orgIdentifier(generateUuid())
                                            .projectIdentifier(generateUuid())
                                            .build();

    byte[] serializedBytes = toBytes(kryo, connectorInfoDTO);
    kryo = new Kryo(new ClassResolver(), new MapReferenceResolver(), new DefaultStreamFactory());
    kryo.addDefaultSerializer(Enum.class, OrdinalBackwardEnumNameSerializer.class);
    kryo.register(ConnectorInfoDTO.class, 500);
    kryo.register(ConnectorType.class, 501);
    ConnectorInfoDTO deserialized = (ConnectorInfoDTO) toObject(kryo, serializedBytes);
    assertThat(deserialized.getConnectorType()).isEqualTo(DOCKER);
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testWriteNameReadOrdinal() {
    HKryo kryo = new HKryo(new ClassResolver());
    kryo.addDefaultSerializer(Enum.class, OrdinalBackwardEnumNameSerializer.class);
    kryo.register(ConnectorInfoDTO.class, 500);
    kryo.register(ConnectorType.class, 501);
    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(DOCKER)
                                            .description(generateUuid())
                                            .identifier(generateUuid())
                                            .name(generateUuid())
                                            .orgIdentifier(generateUuid())
                                            .projectIdentifier(generateUuid())
                                            .build();

    byte[] serializedBytes = toBytes(kryo, connectorInfoDTO);
    kryo = new HKryo(new ClassResolver());
    kryo.register(ConnectorInfoDTO.class, 500);
    kryo.register(ConnectorType.class, 501);
    ConnectorInfoDTO deserialized = (ConnectorInfoDTO) toObject(kryo, serializedBytes);
    assertThat(deserialized.getConnectorType()).isEqualTo(DOCKER);
  }

  private byte[] toBytes(Kryo kryo, Object obj) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Output output = new Output(outputStream);
    kryo.writeClassAndObject(output, obj);
    output.flush();
    return outputStream.toByteArray();
  }

  private Object toObject(Kryo kryo, byte[] bytes) {
    Input input = new Input(bytes);
    Object obj = kryo.readClassAndObject(input);
    input.close();
    return obj;
  }
}
