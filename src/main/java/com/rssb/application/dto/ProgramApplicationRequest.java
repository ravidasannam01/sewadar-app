package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Sewadar zonal ID is required")
    private String sewadarId; // Sewadar zonal ID (String type)

    private String notes;
}

