package io.harness.delegate.task.utils;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Optional;

import static io.harness.rule.OwnerRule.VLAD;
import static org.assertj.core.api.Assertions.assertThat;

public class PhysicalDataCenterUtilsTest {

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldExtractPortFromEmptyHost() {
        Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("");
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldExtractPortFromHost() {
        Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("localhost:8080");
        assertThat(result.get()).isEqualTo(8080);
    }

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldExtractPortFromHostDoubleColumn() {
        Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("local:host:8080");
        assertThat(result.get()).isEqualTo(8080);
    }

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldExtractPortFromHostNotANumber() {
        Optional<Integer> result = PhysicalDataCenterUtils.extractPortFromHost("local:host");
        assertThat(result.isPresent()).isFalse();
    }

    @Test
    @Owner(developers = VLAD)
    @Category(UnitTests.class)
    public void shouldExtractHostnameFromHost() {
        Optional<String> result = PhysicalDataCenterUtils.extractHostnameFromHost("local:host:123");
        assertThat(result.get()).isEqualTo("local:host");
    }
}
