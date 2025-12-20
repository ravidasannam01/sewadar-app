package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for all sewadars attendance summary
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllSewadarsAttendanceSummaryResponse {
    private List<SewadarSummary> sewadars;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SewadarSummary {
        private Long sewadarId;
        private String sewadarName;
        private String mobile;
        private Long beasProgramsCount;
        private Integer beasDaysAttended;
        private Long nonBeasProgramsCount;
        private Integer nonBeasDaysAttended;
        private Long totalProgramsCount;
        private Integer totalDaysAttended;
    }
}

