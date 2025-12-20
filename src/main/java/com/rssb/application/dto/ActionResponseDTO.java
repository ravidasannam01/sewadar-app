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
public class ActionResponseDTO {
    private Long id;
    private Long programId;
    private String programTitle;
    private String title;
    private String description;
    private String actionType;
    private SewadarResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
    private String status;
    private Integer sequenceOrder;
    private Long responseCount; // Number of sewadars who responded
    private Long pendingCount; // Number of sewadars who haven't responded
}

