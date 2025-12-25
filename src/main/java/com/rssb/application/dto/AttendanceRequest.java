package com.rssb.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {
    @NotNull(message = "Program ID is required")
    private Long programId;

    @NotNull(message = "Sewadar zonal IDs are required")
    private List<Long> sewadarIds; // List of sewadar zonal IDs

    @NotNull(message = "Marked by (Incharge zonal ID) is required")
    private Long markedById; // Incharge zonal ID who marked attendance

    private Integer daysParticipated;
    private String notes;
}

