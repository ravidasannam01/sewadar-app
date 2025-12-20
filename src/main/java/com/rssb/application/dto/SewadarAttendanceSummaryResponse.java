package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for sewadar attendance summary with BEAS/non-BEAS breakdown
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarAttendanceSummaryResponse {
    private Long sewadarId;
    private String sewadarName;
    private String mobile;
    
    // BEAS location attendance
    private Long beasProgramsCount;
    private Integer beasDaysAttended;
    private List<AttendanceDetail> beasAttendances;
    
    // Non-BEAS location attendance
    private Long nonBeasProgramsCount;
    private Integer nonBeasDaysAttended;
    private List<AttendanceDetail> nonBeasAttendances;
    
    // Total
    private Long totalProgramsCount;
    private Integer totalDaysAttended;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceDetail {
        private Long programId;
        private String programTitle;
        private String location;
        private String locationType;
        private Boolean attended;
        private Integer daysParticipated;
        private java.time.LocalDateTime markedAt;
    }
}

