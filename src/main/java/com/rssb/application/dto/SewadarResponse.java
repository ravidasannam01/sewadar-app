package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Sewadar response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String dept;
    private String mobile;
    private AddressResponse address;
    private String remarks;
}

