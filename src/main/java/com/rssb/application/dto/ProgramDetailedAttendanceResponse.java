package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Detailed attendance report for a program (one row per sewadar with date columns)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramDetailedAttendanceResponse {
    private Long programId;
    private String programTitle;
    private List<LocalDate> programDates; // Fixed set of dates
    private List<SewadarAttendanceRow> sewadarRows;
    private Long totalSewadars;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SewadarAttendanceRow {
        private Long zonalId;
        private String sewadarName;
        private String mobile;
        private Map<LocalDate, String> dateStatusMap; // Date -> "Present" or "Absent"
    }
}

