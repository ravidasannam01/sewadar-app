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
public class NotificationResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private SewadarResponse droppedSewadar;
    private String notificationType;
    private String message;
    private LocalDateTime createdAt;
    private Boolean resolved;
    private LocalDateTime resolvedAt;
}

