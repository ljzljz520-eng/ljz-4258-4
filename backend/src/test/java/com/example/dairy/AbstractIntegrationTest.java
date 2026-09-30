package com.example.dairy;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest(properties = {"app.worker.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers
public abstract class AbstractIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    DataSource dataSource;

    @BeforeEach
    void resetSeedData() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.createStatement().execute("""
                TRUNCATE TABLE
                  audit_logs, reviews, evidence_snapshots, pressure_mapping_revisions,
                  comparison_dependencies, comparison_reasons, comparisons,
                  pressure_readings, particle_distributions, samples, import_files, import_jobs,
                  pressure_calibration_mappings, batch_segments, measurement_points,
                  instrument_algorithm_versions, sampling_lines, product_batches, valve_groups
                RESTART IDENTITY CASCADE
            """);
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__seed_reference_and_examples.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V3__seed_completed_imports.sql"));
        }
    }
}
