package com.rssb.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarFormSubmissionRequest {
    @NotNull(message = "Program ID is required")
    private Long programId;

    // Name removed - available from sewadar relationship
    private LocalDateTime startingDateTimeFromHome;
    private LocalDateTime reachingDateTimeToHome;
    private LocalDateTime onwardTrainFlightDateTime;
    private String onwardTrainFlightNo;
    private LocalDateTime returnTrainFlightDateTime;
    private String returnTrainFlightNo;
    private String stayInHotel;
    private String stayInPandal;
}

