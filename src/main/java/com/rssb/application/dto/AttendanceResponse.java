package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private SewadarResponse sewadar;
    private LocalDate attendanceDate; // The specific date when attendance was marked (from programDate.programDate)
    private Long programDateId; // ID of the program_date record
    private LocalDateTime markedAt; // When this attendance was marked
    private String notes;
}

