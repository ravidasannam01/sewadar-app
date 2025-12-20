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

    @NotNull(message = "Sewadar IDs are required")
    private List<Long> sewadarIds;

    @NotNull(message = "Marked by (Incharge ID) is required")
    private Long markedById;

    private Integer daysParticipated;
    private String notes;
}

