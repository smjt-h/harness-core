package io.harness.serializer;

import static com.esotericsoftware.kryo.Kryo.NULL;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrdinalBackwardEnumNameSerializer extends Serializer<Enum> {
  private final Class<? extends Enum> enumType;
  private final Serializer stringSerializer;
  private Object[] enumConstants;

  public OrdinalBackwardEnumNameSerializer(Kryo kryo, Class<? extends Enum> type) {
    this.enumType = type;
    this.stringSerializer = kryo.getSerializer(String.class);
    setImmutable(true);
    setAcceptsNull(true);
    enumConstants = type.getEnumConstants();
  }

  public void write(Kryo kryo, Output output, Enum object) {
    if (object == null) {
      output.writeVarInt(NULL, true);
      return;
    }
    output.writeVarInt(object.ordinal() + 1, true);
  }

  public Enum read(Kryo kryo, Input input, Class<Enum> type) {
    try {
      int ordinal = input.readVarInt(true);
      if (ordinal == NULL) {
        return null;
      }
      ordinal--;
      if (ordinal < 0 || ordinal > enumConstants.length - 1) {
        throw new KryoException("Invalid ordinal for enum \"" + type.getName() + "\": " + ordinal);
      }
      return (Enum) enumConstants[ordinal];
    } catch (Exception ordinalException) {
      String name = kryo.readObject(input, String.class, stringSerializer);
      try {
        return Enum.valueOf(enumType, name);
      } catch (Exception nameException) {
        log.error("could not deserialize with either ordinal or name", nameException, ordinalException);
        throw new KryoException("Invalid name for enum \"" + enumType.getName() + "\": " + name, nameException);
      }
    }
  }
}
