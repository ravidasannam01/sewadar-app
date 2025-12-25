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
    private String location; // If location is 'BEAS', it's BEAS location, otherwise NON_BEAS

    @NotNull(message = "Program dates are required")
    private List<LocalDate> programDates; // Multiple dates for one program

    private String status;
    private Integer maxSewadars;
    @NotNull(message = "Created by (Incharge ID) is required")
    private Long createdById;
}

