package com.rssb.application.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic SQL Query Controller for testing and debugging purposes.
 * 
 * WARNING: This endpoint has NO authentication and allows direct SQL execution.
 * Use only in development/testing environments. DO NOT enable in production.
 * 
 * This controller allows executing any SQL query and returns results as JSON.
 */
@RestController
@RequestMapping("/api/sql-query")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SqlQueryController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Execute a SQL query and return results.
     * 
     * @param request SQL query request containing the query string
     * @return Query results as JSON
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeQuery(@RequestBody SqlQueryRequest request) {
        log.warn("SQL Query executed: {}", request.getQuery());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String query = request.getQuery();
            if (query == null || query.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "Query cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Normalize query (trim whitespace)
            query = query.trim();
            
            // Check if it's a SELECT query (for safety, though we allow all queries)
            boolean isSelect = query.toUpperCase().startsWith("SELECT");
            
            if (isSelect) {
                // Execute SELECT query and return results
                List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
                
                response.put("success", true);
                response.put("query", query);
                response.put("rowCount", results.size());
                response.put("results", results);
                response.put("message", "Query executed successfully");
            } else {
                // Execute UPDATE, INSERT, DELETE, etc.
                int rowsAffected = jdbcTemplate.update(query);
                
                response.put("success", true);
                response.put("query", query);
                response.put("rowsAffected", rowsAffected);
                response.put("message", "Query executed successfully. Rows affected: " + rowsAffected);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error executing SQL query: {}", request.getQuery(), e);
            
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            response.put("query", request.getQuery());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Execute a SELECT query and return results (GET method for simple queries).
     * Note: Query must be URL-encoded.
     * 
     * @param query SQL query string (URL-encoded)
     * @return Query results as JSON
     */
    @GetMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeQueryGet(@RequestParam String query) {
        SqlQueryRequest request = new SqlQueryRequest();
        request.setQuery(query);
        return executeQuery(request);
    }

    /**
     * Get table names in the database.
     * 
     * @return List of table names
     */
    @GetMapping("/tables")
    public ResponseEntity<Map<String, Object>> getTables() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // PostgreSQL query to get all tables
            String sql = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = 'public' " +
                        "ORDER BY table_name";
            
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(sql);
            
            response.put("success", true);
            response.put("tables", tables);
            response.put("count", tables.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching tables", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Get column information for a specific table.
     * 
     * @param tableName Table name
     * @return Column information
     */
    @GetMapping("/table/{tableName}/columns")
    public ResponseEntity<Map<String, Object>> getTableColumns(@PathVariable String tableName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // PostgreSQL query to get column information
            String sql = "SELECT column_name, data_type, is_nullable, column_default " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = 'public' AND table_name = ? " +
                        "ORDER BY ordinal_position";
            
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(sql, tableName);
            
            response.put("success", true);
            response.put("tableName", tableName);
            response.put("columns", columns);
            response.put("columnCount", columns.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching columns for table: {}", tableName, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Request DTO for SQL queries
     */
    @Data
    public static class SqlQueryRequest {
        private String query;
    }
}

