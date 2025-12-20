package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for application with prioritization metrics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrioritizedApplicationResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private SewadarResponse sewadar;
    private LocalDateTime appliedAt;
    private String status;
    
    // Prioritization metrics
    private Long totalAttendanceCount; // Total programs attended
    private Long beasAttendanceCount; // BEAS programs attended
    private Long nonBeasAttendanceCount; // Non-BEAS programs attended
    private Integer totalDaysAttended; // Total days attended across all programs
    private Integer beasDaysAttended; // Days attended at BEAS locations
    private Integer nonBeasDaysAttended; // Days attended at non-BEAS locations
    private String profession;
    private java.time.LocalDate joiningDate;
    private Long priorityScore; // Calculated priority score
}

