package io.harness.serializer.kryo;

import io.harness.cdng.service.beans.ServiceConfigOutcome;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.serializer.KryoRegistrar;

import com.esotericsoftware.kryo.Kryo;

public class NGEntitiesKryoRegistrar implements KryoRegistrar {
    @Override
    public void register(Kryo kryo) {
        kryo.register(ServiceConfigOutcome.class, 12508);
        kryo.register(ServiceEntity.class, 22002);
    }
}
