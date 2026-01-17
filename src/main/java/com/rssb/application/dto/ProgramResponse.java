package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String locationType; // BEAS or NON_BEAS
    private List<LocalDate> programDates; // Multiple dates
    private String status;
    private Integer maxSewadars;
    private SewadarResponse createdBy;
    private Long applicationCount;
    private Long dropRequestsCount; // Count of pending drop requests
}

