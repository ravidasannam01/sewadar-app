package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for application dashboard with filters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDashboardResponse {
    private List<ApplicationDashboardItem> applications;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationDashboardItem {
        private Long applicationId;
        private Long sewadarZonalId;
        private String sewadarName;
        private String mobile;
        private String status;
        private java.time.LocalDateTime appliedAt;
    }
}

