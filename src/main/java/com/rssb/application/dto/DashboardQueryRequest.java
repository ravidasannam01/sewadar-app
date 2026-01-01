package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Generic request DTO for dashboard queries with filters, sorting, and pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardQueryRequest {
    
    // Pagination
    private Integer page = 0; // 0-based page number
    private Integer size = 25; // Page size
    
    // Filters
    private List<String> languages; // Languages to filter by
    private String languageMatchType; // "ALL" or "ANY" - match all languages or any language
    private String location; // Filter by location
    private LocalDate joiningDateFrom; // Joining date range start
    private LocalDate joiningDateTo; // Joining date range end
    
    // Sorting
    private String sortBy; // "totalPrograms", "totalDays", "beasDays", "nonBeasDays", "joiningDate"
    private String sortOrder; // "ASC" or "DESC"
    
    // For application dashboard
    private Long programId; // Filter applications by program
    private List<String> statuses; // Filter by application statuses [PENDING, APPROVED, etc.]
}

