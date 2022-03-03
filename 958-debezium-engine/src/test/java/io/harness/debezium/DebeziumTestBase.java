package io.harness.debezium;

import io.harness.CategoryTest;
import io.harness.rule.LifecycleRule;

import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class DebeziumTestBase extends CategoryTest {
  @Rule public LifecycleRule lifecycleRule = new LifecycleRule();
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule public DebeziumRule debeziumRule = new DebeziumRule(lifecycleRule.getClosingFactory());
}
