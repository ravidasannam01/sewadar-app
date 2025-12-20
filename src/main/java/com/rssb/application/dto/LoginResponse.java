package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token; // JWT token
    @Builder.Default
    private String tokenType = "Bearer";
    private SewadarResponse sewadar; // User details
    private Long expiresIn; // Token expiration in seconds
}

