package com.rssb.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProgramRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    private String locationType; // BEAS or NON_BEAS (defaults to NON_BEAS if not provided)

    @NotNull(message = "Program dates are required")
    private List<LocalDate> programDates; // Multiple dates for one program

    private String status;
    private Integer maxSewadars;
    @NotNull(message = "Created by (Incharge ID) is required")
    private Long createdById;
}

