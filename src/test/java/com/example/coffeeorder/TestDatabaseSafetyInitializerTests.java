package com.example.coffeeorder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestDatabaseSafetyInitializerTests {
    @ParameterizedTest
    @ValueSource(strings = {
            "jdbc:h2:mem:coffee_order;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
            "jdbc:mysql://localhost:3306/coffee_order_codex_test_20261007",
            "jdbc:mysql://127.0.0.1:3306/coffee_order_codex_test_20261007"})
    void acceptsOnlyDesignatedDatabases(String url) {
        try (var context = context(url)) {
            assertThatCode(() -> new TestDatabaseSafetyInitializer().initialize(context)).doesNotThrowAnyException();
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "", "jdbc:mysql://localhost:3306/coffee_order",
            "jdbc:mysql://localhost:3306/another_test",
            "jdbc:mysql://localhost:3306/coffee_order_codex_test_20261007_extra",
            "jdbc:mysql://localhost:3306/coffee_order_codex_test_20261007?sessionVariables=sql_mode=''",
            "jdbc:mysql://remote-host:3306/coffee_order_codex_test_20261007",
            "jdbc:mysql://localhost:3307/coffee_order_codex_test_20261007",
            "jdbc:h2:file:./coffee_order"})
    void rejectsUnsafeTargetsBeforeContextRefresh(String url) {
        try (var context = context(url)) {
            assertThatThrownBy(() -> new TestDatabaseSafetyInitializer().initialize(context))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("dedicated local");
        }
    }

    private GenericApplicationContext context(String url) {
        var context = new GenericApplicationContext();
        var environment = new MockEnvironment();
        if (url != null) environment.setProperty("spring.datasource.url", url);
        context.setEnvironment(environment);
        return context;
    }
}
