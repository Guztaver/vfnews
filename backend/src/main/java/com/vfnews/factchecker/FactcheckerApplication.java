package com.vfnews.factchecker;

import com.vfnews.factchecker.config.DataSourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FactcheckerApplication {

    public static void main(String[] args) {
        String dbUrl = System.getenv().getOrDefault("DATABASE_URL",
                System.getProperty("DATABASE_URL", "jdbc:sqlite:db.sqlite3"));
        DataSourceConfig.ensureValidSqliteFile(dbUrl);
        SpringApplication.run(FactcheckerApplication.class, args);
    }

}
