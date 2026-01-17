package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarFormSubmissionResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private Long sewadarId;
    private String sewadarName;
    private String name;
    private LocalDateTime startingDateTimeFromHome;
    private LocalDateTime reachingDateTimeToHome;
    private LocalDateTime onwardTrainFlightDateTime;
    private String onwardTrainFlightNo;
    private LocalDateTime returnTrainFlightDateTime;
    private String returnTrainFlightNo;
    private String stayInHotel;
    private String stayInPandal;
    private LocalDateTime submittedAt;
}

