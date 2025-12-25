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

    private String location; // Location/center (replaces dept)

    private String mobile;

    // Address fields (optional - user can enter address directly)
    private String address1;
    private String address2;
    private String email;

    private String remarks;

    // Role and additional fields
    private String role; // INCHARGE or SEWADAR (not set during creation)
    private java.time.LocalDate joiningDate;
    private String profession;
    private String password; // Password for login (optional, defaults to "password123")
    
    // New fields
    private java.time.LocalDate dateOfBirth;
    private String emergencyContact;
    private String emergencyContactRelationship;
    private String photoUrl;
    private java.util.List<String> languages; // Languages known
}

