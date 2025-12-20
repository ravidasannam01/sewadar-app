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
public class ProgramSelectionResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private SewadarResponse sewadar;
    private SewadarResponse selectedBy;
    private LocalDateTime selectedAt;
    private String status;
    private Integer priorityScore;
    private String selectionReason;
}

