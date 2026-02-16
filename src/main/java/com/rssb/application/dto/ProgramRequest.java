package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Location is required")
    private String location; // If location is 'BEAS', it's BEAS location, otherwise NON_BEAS

    @NotNull(message = "Program dates are required")
    private List<LocalDate> programDates; // Multiple dates for one program

    private String status; // scheduled, active, cancelled (default: scheduled)
    private Integer maxSewadars;

    /**
     * Application deadline (server-local time). Nullable = no deadline.
     * Expected JSON example: \"2026-02-16T18:30:00\"
     */
    private LocalDateTime lastDateToApply;

    /**
     * Form submission deadline (server-local time). Nullable = no deadline.
     * Expected JSON example: \"2026-02-17T18:30:00\"
     */
    private LocalDateTime lastDateToSubmitForm;

    @NotBlank(message = "Created by (Incharge zonal ID) is required")
    private String createdById; // Incharge zonal ID (String type)
}

