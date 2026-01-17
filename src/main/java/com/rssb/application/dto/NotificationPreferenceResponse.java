package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceResponse {
    private Long id;
    private Integer nodeNumber;
    private String nodeName;
    private String notificationMessage;
    private Boolean enabled;
}

