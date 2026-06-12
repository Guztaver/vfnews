package com.vfnews.factchecker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${DATABASE_URL:jdbc:sqlite:db.sqlite3}")
    private String databaseUrl;

    @Value("${DATABASE_DRIVER:org.sqlite.JDBC}")
    private String driver;

    @Bean
    @Primary
    public DataSource dataSource() {
        String url = databaseUrl;
        String user = System.getenv("DATABASE_USERNAME");
        String pass = System.getenv("DATABASE_PASSWORD");

        if (url.startsWith("postgres://")) {
            String remainder = url.substring("postgres://".length());
            int atIndex = remainder.indexOf('@');
            if (atIndex > 0) {
                String userInfo = remainder.substring(0, atIndex);
                remainder = remainder.substring(atIndex + 1);
                int colonIdx = userInfo.indexOf(':');
                if (colonIdx > 0) {
                    user = userInfo.substring(0, colonIdx);
                    pass = userInfo.substring(colonIdx + 1);
                } else {
                    user = userInfo;
                }
            }
            url = "jdbc:postgresql://" + remainder;
            driver = "org.postgresql.Driver";
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setDriverClassName(driver);
        if (user != null && !user.isBlank()) config.setUsername(user);
        if (pass != null && !pass.isBlank()) config.setPassword(pass);
        config.setMaximumPoolSize(5);

        return new HikariDataSource(config);
    }

    public static void ensureValidSqliteFile(String url) {
        if (!url.startsWith("jdbc:sqlite:")) return;
        String path = url.substring("jdbc:sqlite:".length());
        File dbFile = new File(path);
        File parentDir = dbFile.getAbsoluteFile().getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        boolean needsRecreate = false;
        if (dbFile.exists()) {
            if (dbFile.length() == 0) {
                needsRecreate = true;
            } else {
                try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
                    c.createStatement().execute("SELECT 1");
                } catch (Exception e) {
                    needsRecreate = true;
                }
            }
        }

        if (needsRecreate) {
            boolean deleted = dbFile.delete();
            if (!deleted) {
                log.error("Failed to delete corrupted database file: {}", dbFile.getAbsolutePath());
                throw new RuntimeException("Cannot delete corrupted SQLite database: " + dbFile.getAbsolutePath());
            }
        }

        if (!dbFile.exists() || needsRecreate) {
            try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
                c.createStatement().execute("CREATE TABLE IF NOT EXISTS _db_init_check (id INTEGER PRIMARY KEY)");
            } catch (Exception e) {
                log.error("Failed to initialize SQLite database: {}", dbFile.getAbsolutePath(), e);
                throw new RuntimeException("Cannot initialize SQLite database: " + dbFile.getAbsolutePath(), e);
            }
            log.info("SQLite database initialized: {}", dbFile.getAbsolutePath());
        }
    }

}
