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
public class ProgramApplicationRequest {
    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotNull(message = "Sewadar ID is required")
    private Long sewadarId;

    private String notes;
}

