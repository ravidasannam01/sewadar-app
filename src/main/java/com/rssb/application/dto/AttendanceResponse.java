package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Boolean attended;
    private Long markedBy;
    private LocalDateTime markedAt;
    private String notes;
    private Integer daysParticipated;
}

