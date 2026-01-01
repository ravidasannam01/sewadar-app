package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Detailed attendance report for a sewadar (one row per program-date)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarDetailedAttendanceResponse {
    private Long sewadarId;
    private String sewadarName;
    private String mobile;
    private List<AttendanceRecord> records;
    private Long totalRecords;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceRecord {
        private Long programId;
        private String programTitle;
        private String programLocation;
        private LocalDate attendanceDate;
        private String status; // "Present" or "Absent"
    }
}

