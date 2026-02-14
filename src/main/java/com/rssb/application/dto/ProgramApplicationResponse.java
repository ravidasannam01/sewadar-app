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
public class ProgramApplicationResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private SewadarResponse sewadar;
    private LocalDateTime appliedAt;
    private String status;
    private String notes;
    private LocalDateTime dropRequestedAt;
    private LocalDateTime dropApprovedAt;
    private String dropApprovedBy; // Incharge zonal_id who approved drop (String type)
}

