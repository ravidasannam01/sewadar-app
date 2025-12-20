package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for Schedule response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {
    private Long id;
    private String scheduledPlace;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String scheduledMedium;
    private SewadarResponse attendedBy;
}

