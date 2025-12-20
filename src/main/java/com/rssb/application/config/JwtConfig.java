package com.rssb.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret = "your-secret-key-change-this-in-production-minimum-256-bits-required-for-security";
    private Long expiration = 86400000L; // 24 hours in milliseconds
}

