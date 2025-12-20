package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionRequest {
    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String actionType; // TRAVEL_DETAILS, ACCOMMODATION, DOCUMENTS, etc.

    @NotNull(message = "Created by (Incharge ID) is required")
    private Long createdById;

    private LocalDateTime dueDate;

    private Integer sequenceOrder; // For ordering actions
}

