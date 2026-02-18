package com.rssb.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramWorkflowResponse {
    private Long id;
    private Long programId;
    private String programTitle;
    private Integer currentNode;
    private String currentNodeName;
    private Boolean formReleased;
    private Boolean detailsCollected;
    private Boolean archived;
    private java.time.LocalDateTime archivedAt;
}

