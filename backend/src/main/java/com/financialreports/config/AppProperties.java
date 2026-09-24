package com.financialreports.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(Jwt jwt, List<String> corsOrigins, Admin admin, boolean demoSeed) {

    public record Jwt(String secret, Duration expiration) {}

    public record Admin(String email, String password) {}
}
