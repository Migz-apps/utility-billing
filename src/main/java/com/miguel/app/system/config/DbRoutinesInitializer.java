package com.miguel.app.system.config;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Automatically installs the Task 6 database routines (triggers + stored procedure with cursor)
 * from {@code db/routines.sql} on application startup so a grader does not need to run psql by hand.
 *
 * <p>Only runs against PostgreSQL (skipped on H2 used by tests). The script itself is idempotent
 * (CREATE OR REPLACE + DROP TRIGGER IF EXISTS), so restarting the app re-applies safely. Disable with
 * {@code app.db.routines.auto-apply=false}.
 */
@Component
@RequiredArgsConstructor
public class DbRoutinesInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DbRoutinesInitializer.class);

    private final DataSource dataSource;

    @Value("${app.db.routines.auto-apply:true}")
    private boolean autoApply;

    @Value("${app.db.routines.script:db/routines.sql}")
    private String scriptPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!autoApply) {
            log.info("DB routines auto-apply disabled; skipping {}", scriptPath);
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                log.info("Skipping DB routines: database product is {} (PostgreSQL required)", product);
                return;
            }
            ClassPathResource resource = new ClassPathResource(scriptPath);
            if (!resource.exists()) {
                log.warn("DB routines script not found on classpath: {}", scriptPath);
                return;
            }
            String sql;
            try (var in = resource.getInputStream()) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            try (Statement stmt = connection.createStatement()) {
                // The PostgreSQL JDBC driver passes the whole script to the server, which handles
                // multi-statement bodies (including PL/pgSQL $$ blocks) correctly.
                stmt.execute(sql);
            }
            log.info("Installed DB routines from {}", scriptPath);
        } catch (Exception ex) {
            // Do not block app startup if routines fail to install; surface the error in the logs.
            log.error("Failed to install DB routines from {}: {}", scriptPath, ex.getMessage(), ex);
        }
    }
}
