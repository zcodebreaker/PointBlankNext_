package com.pointblank.api.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, EmailVerification emailVerification) {

    public record Jwt(
            String secret,
            String issuer,
            Duration accessTokenTtl,
            Duration rememberTokenTtl) {
    }

    public record Cors(String allowedOrigins) {
    }

    public record EmailVerification(
            String publicUrl,
            String fromAddress,
            Duration tokenTtl) {
    }
}

