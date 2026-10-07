package com.example.coffeeorder;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Set;


public class TestDatabaseSafetyInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Set<String> ALLOWED_URLS = Set.of(
            "jdbc:h2:mem:coffee_order;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
            "jdbc:mysql://localhost:3306/coffee_order_codex_test_20261007",
            "jdbc:mysql://127.0.0.1:3306/coffee_order_codex_test_20261007");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getProperty("spring.datasource.url");
        if (url == null || !ALLOWED_URLS.contains(url)) {

            throw new IllegalStateException("Tests require the isolated H2 database or the dedicated local "
                    + "coffee_order_codex_test_20261007 MySQL database, without URL parameters.");
        }
    }
}
