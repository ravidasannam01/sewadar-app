package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramNotificationPreferenceResponse {
    private Long id;
    private Long programId;
    private Integer nodeNumber;
    private String nodeName;
    private Boolean enabled; // null = use global, true/false = override
    private Boolean effectiveEnabled; // Final effective value (program-level or global)
}

