package com.rssb.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionResponseRequest {
    @NotNull(message = "Action ID is required")
    private Long actionId;

    @NotNull(message = "Sewadar ID is required")
    private Long sewadarId;

    private String responseData; // JSON or text response

    private String notes;
}

