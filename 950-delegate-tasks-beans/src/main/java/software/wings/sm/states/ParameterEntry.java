/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sgurubelli on 8/31/17.
 */
@Data
@NoArgsConstructor
public class ParameterEntry {
  @Attributes(title = "Name") String key;
  @Attributes(title = "Value") String value;
}
