/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.kryo.YamlKryoRegistrar;
import io.harness.serializer.morphia.YamlMorphiaRegistrar;

import com.google.common.collect.ImmutableSet;
import lombok.experimental.UtilityClass;

@UtilityClass
public class YamlBeansModuleRegistrars {
  public final ImmutableSet<Class<? extends KryoRegistrar>> kryoRegistrars =
      ImmutableSet.<Class<? extends KryoRegistrar>>builder()
          .addAll(TimeoutEngineRegistrars.kryoRegistrars)
          .add(YamlKryoRegistrar.class)
          .build();

  public static final ImmutableSet<Class<? extends MorphiaRegistrar>> morphiaRegistrars =
      ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
          .addAll(TimeoutEngineRegistrars.morphiaRegistrars)
          .add(YamlMorphiaRegistrar.class)
          .build();
}
