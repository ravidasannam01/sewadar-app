package com.rssb.application.util;

import com.rssb.application.config.LoggingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Utility class for logging application actions.
 * Respects the logging configuration to enable/disable logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActionLogger {
    
    private final LoggingConfig loggingConfig;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Log an action performed by a user.
     * 
     * @param action Action name (e.g., "CREATE_PROGRAM", "APPROVE_APPLICATION")
     * @param userId User ID (zonalId) performing the action
     * @param userRole Role of the user
     * @param details Additional details about the action
     */
    public void logAction(String action, String userId, String userRole, Map<String, Object> details) {
        if (!loggingConfig.isActionLoggingEnabled()) {
            return;
        }
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format("[ACTION] %s | User: %s (%s) | Time: %s",
                action, userId, userRole, LocalDateTime.now().format(FORMATTER)));
        
        if (details != null && !details.isEmpty()) {
            logMessage.append(" | Details: ");
            details.forEach((key, value) -> {
                logMessage.append(String.format("%s=%s, ", key, value));
            });
            // Remove trailing comma and space
            if (logMessage.length() > 0) {
                logMessage.setLength(logMessage.length() - 2);
            }
        }
        
        log.info(logMessage.toString());
    }
    
    /**
     * Log an action with a simple message.
     * 
     * @param action Action name
     * @param userId User ID performing the action
     * @param userRole Role of the user
     * @param message Simple message
     */
    public void logAction(String action, String userId, String userRole, String message) {
        if (!loggingConfig.isActionLoggingEnabled()) {
            return;
        }
        
        log.info("[ACTION] {} | User: {} ({}) | Time: {} | {}", 
                action, userId, userRole, LocalDateTime.now().format(FORMATTER), message);
    }
    
    /**
     * Log a request (if request/response logging is enabled).
     * 
     * @param method HTTP method
     * @param endpoint Endpoint path
     * @param userId User ID making the request
     * @param requestData Request data
     */
    public void logRequest(String method, String endpoint, String userId, Object requestData) {
        if (!loggingConfig.isRequestResponseLoggingEnabled()) {
            return;
        }
        
        log.debug("[REQUEST] {} {} | User: {} | Data: {}", method, endpoint, userId, requestData);
    }
    
    /**
     * Log a response (if request/response logging is enabled).
     * 
     * @param method HTTP method
     * @param endpoint Endpoint path
     * @param statusCode HTTP status code
     * @param responseData Response data
     */
    public void logResponse(String method, String endpoint, int statusCode, Object responseData) {
        if (!loggingConfig.isRequestResponseLoggingEnabled()) {
            return;
        }
        
        log.debug("[RESPONSE] {} {} | Status: {} | Data: {}", method, endpoint, statusCode, responseData);
    }
    
    /**
     * Log performance metrics (if performance logging is enabled).
     * 
     * @param action Action name
     * @param durationMs Duration in milliseconds
     */
    public void logPerformance(String action, long durationMs) {
        if (!loggingConfig.isPerformanceLoggingEnabled()) {
            return;
        }
        
        log.info("[PERFORMANCE] {} | Duration: {}ms", action, durationMs);
    }
    
    /**
     * Log an error with context.
     * 
     * @param action Action that failed
     * @param userId User ID (if available)
     * @param error Error message or exception
     */
    public void logError(String action, String userId, String error) {
        log.error("[ERROR] {} | User: {} | Error: {} | Time: {}", 
                action, userId != null ? userId : "SYSTEM", error, LocalDateTime.now().format(FORMATTER));
    }
}

