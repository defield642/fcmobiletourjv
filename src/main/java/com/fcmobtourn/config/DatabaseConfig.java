package com.fcmobtourn.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(@Value("${DATABASE_URL:}") String databaseUrl) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return DataSourceBuilder.create()
                    .type(HikariDataSource.class)
                    .url("jdbc:h2:mem:fcmobile;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
                    .build();
        }

        String configured = databaseUrl.trim();
        if (configured.startsWith("jdbc:")) {
            return DataSourceBuilder.create()
                    .type(HikariDataSource.class)
                    .url(configured)
                    .build();
        }

        URI uri = URI.create(configured);
        if (!"postgresql".equalsIgnoreCase(uri.getScheme())
                && !"postgres".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("DATABASE_URL must use postgresql:// or jdbc:postgresql://");
        }

        String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/postgres" : uri.getRawPath();
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
                + path
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        if (uri.getRawUserInfo() != null) {
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            dataSource.setUsername(decode(credentials[0]));
            if (credentials.length == 2) {
                dataSource.setPassword(decode(credentials[1]));
            }
        }
        return dataSource;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
