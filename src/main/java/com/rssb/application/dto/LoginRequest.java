package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank(message = "Zonal ID is required")
    private String zonalId; // Zonal ID is used as username (can be string to handle leading zeros)

    @NotBlank(message = "Password is required")
    private String password;
}

