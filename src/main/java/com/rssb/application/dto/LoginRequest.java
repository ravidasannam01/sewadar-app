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
    @NotBlank(message = "Mobile number is required")
    private String mobile; // Mobile number is used as username

    @NotBlank(message = "Password is required")
    private String password;
}

