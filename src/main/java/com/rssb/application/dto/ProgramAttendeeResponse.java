package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for approved sewadars who can have attendance marked for a program
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramAttendeeResponse {
    private Long zonalId;
    private String firstName;
    private String lastName;
    private String mobile;
    private Long applicationId; // ProgramApplication ID
    private String applicationStatus; // Usually "APPROVED"
}

