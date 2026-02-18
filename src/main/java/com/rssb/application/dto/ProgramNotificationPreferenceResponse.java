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
    private String message; // null = use global message, non-null = custom program-level message
    private String defaultMessage; // Global/default message for this node (for reset functionality)
    private Boolean isCustomMessage; // true if message is program-specific (not null), false if using default
}

