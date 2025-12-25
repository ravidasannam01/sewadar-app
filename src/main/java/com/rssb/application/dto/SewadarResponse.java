package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for Sewadar response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarResponse {
    private Long zonalId; // Renamed from id
    private String firstName;
    private String lastName;
    private String location; // Replaces dept
    private String mobile;
    private AddressResponse address;
    private String remarks;
    private String role;
    private LocalDate joiningDate;
    private String profession;
    private LocalDate dateOfBirth;
    private String emergencyContact;
    private String emergencyContactRelationship;
    private String photoUrl;
    private String aadharNumber; // 12-digit Aadhar number
    private List<String> languages; // Languages known
}

