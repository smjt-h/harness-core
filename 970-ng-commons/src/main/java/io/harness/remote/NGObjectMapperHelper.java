/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.remote;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.utils.NGObjectMapperHelper.configureNGObjectMapper;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnexpectedException;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.jackson.Jackson;
import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class NGObjectMapperHelper {
  public static final ObjectMapper NG_DEFAULT_OBJECT_MAPPER = configureNGObjectMapper(Jackson.newObjectMapper());

  public static Object clone(Object object) {
    try {
      return NG_DEFAULT_OBJECT_MAPPER.readValue(NG_DEFAULT_OBJECT_MAPPER.writeValueAsString(object), object.getClass());
    } catch (Exception exception) {
      throw new UnexpectedException("Exception occurred while copying object.", exception);
    }
  }
}
