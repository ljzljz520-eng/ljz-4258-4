package com.dairy.homogenization.service;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostgresMigrationTest {
    @Test
    void flywayMigrationsApplyToRealPostgres() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().start()) {
            Flyway.configure()
                    .dataSource(pg.getJdbcUrl("postgres", "postgres"), "postgres", "")
                    .schemas("public")
                    .load()
                    .migrate();
            try (Connection c = DriverManager.getConnection(pg.getJdbcUrl("postgres", "postgres"), "postgres", "");
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("""
                         select count(*) from information_schema.tables
                         where table_schema='public' and table_name in
                         ('valve_groups','pressure_mapping_versions','samples','comparison_candidates','evidence_snapshots','import_files')
                         """)) {
                rs.next();
                assertTrue(rs.getInt(1) >= 6);
            }
        }
    }
}
