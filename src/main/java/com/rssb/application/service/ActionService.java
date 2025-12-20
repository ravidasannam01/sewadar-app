package com.rssb.application.service;

import com.rssb.application.dto.ActionRequest;
import com.rssb.application.dto.ActionResponseDTO;

import java.util.List;

public interface ActionService {
    ActionResponseDTO createAction(ActionRequest request);
    ActionResponseDTO getActionById(Long id);
    List<ActionResponseDTO> getActionsByProgram(Long programId);
    List<ActionResponseDTO> getActionsForSewadar(Long sewadarId, Long programId);
    ActionResponseDTO updateAction(Long id, ActionRequest request);
    ActionResponseDTO updateActionOrder(Long id, Integer newOrder);
    void deleteAction(Long id);
    void reorderActions(Long programId, List<Long> actionIdsInOrder);
}

