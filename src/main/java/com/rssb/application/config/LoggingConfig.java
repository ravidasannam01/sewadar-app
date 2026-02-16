package com.rssb.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for application logging.
 * Controls whether detailed action logging is enabled.
 */
@Configuration
@ConfigurationProperties(prefix = "app.logging")
@Data
public class LoggingConfig {
    
    /**
     * Enable detailed action logging for frontend operations.
     * When enabled, logs all actions like program creation, application approval, etc.
     * Default: true
     */
    private boolean actionLoggingEnabled = true;
    
    /**
     * Enable request/response logging.
     * When enabled, logs request details and response data.
     * Default: false (can be verbose)
     */
    private boolean requestResponseLoggingEnabled = false;
    
    /**
     * Enable performance logging.
     * When enabled, logs execution time for operations.
     * Default: false
     */
    private boolean performanceLoggingEnabled = false;
}

