package com.vfnews.factchecker.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Converts Coolify's postgres://DATABASE_URL to JDBC format.
 * Coolify provides DATABASE_URL=postgres://user:pass@host:port/db
 * but JDBC requires jdbc:postgresql://host:port/db
 */
@Configuration
public class DataSourceConfig {

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
}
