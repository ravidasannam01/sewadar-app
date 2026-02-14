package com.rssb.application.dto;

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
public class AttendanceRequest {
    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotNull(message = "Program date is required")
    private LocalDate programDate; // The specific date for which attendance is being marked

    @NotNull(message = "Sewadar zonal IDs are required")
    private List<String> sewadarIds; // List of sewadar zonal IDs to mark attendance for (String type)

    private String notes; // Optional notes for this attendance marking (applied to all records)
}

