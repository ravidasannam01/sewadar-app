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
public class ActionResponseResponse {
    private Long id;
    private Long actionId;
    private String actionTitle;
    private SewadarResponse sewadar;
    private String responseData;
    private String status;
    private LocalDateTime submittedAt;
    private String notes;
}

