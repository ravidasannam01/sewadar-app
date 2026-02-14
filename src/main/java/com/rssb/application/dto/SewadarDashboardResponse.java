package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for sewadar dashboard query with pagination
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarDashboardResponse {
    private List<SewadarDashboardItem> sewadars;
    private Long totalElements;
    private Integer totalPages;
    private Integer currentPage;
    private Integer pageSize;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SewadarDashboardItem {
        private String zonalId;
        private String firstName;
        private String lastName;
        private String mobile;
        private String location;
        private String profession;
        private LocalDate joiningDate;
        private List<String> languages;
        
        // Attendance statistics
        private Long totalProgramsCount;
        private Long totalDaysAttended;
        private Long beasProgramsCount;
        private Long beasDaysAttended;
        private Long nonBeasProgramsCount;
        private Long nonBeasDaysAttended;
    }
}

