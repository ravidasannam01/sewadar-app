package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating/updating a Sewadar.
 * Address fields are included directly instead of addressId for better UX.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String dept;

    private String mobile;

    // Address fields (optional - user can enter address directly)
    private String address1;
    private String address2;
    private String email;

    private String remarks;
}

