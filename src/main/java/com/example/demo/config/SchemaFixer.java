package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SchemaFixer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("ALTER TABLE products ALTER COLUMN description TYPE TEXT;");
            jdbcTemplate.execute("ALTER TABLE products ALTER COLUMN image_url TYPE TEXT;");
            System.out.println("SCHEMA FIXER: Successfully altered columns to TEXT.");
        } catch (Exception e) {
            System.out.println("SCHEMA FIXER: Error or already applied: " + e.getMessage());
        }
    }
}
