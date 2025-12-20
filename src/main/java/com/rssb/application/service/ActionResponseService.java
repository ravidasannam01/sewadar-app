package com.rssb.application.service;

import com.rssb.application.dto.ActionResponseRequest;
import com.rssb.application.dto.ActionResponseResponse;

import java.util.List;

public interface ActionResponseService {
    ActionResponseResponse submitResponse(ActionResponseRequest request);
    ActionResponseResponse getResponseById(Long id);
    List<ActionResponseResponse> getResponsesByAction(Long actionId);
    List<ActionResponseResponse> getResponsesBySewadar(Long sewadarId);
    ActionResponseResponse updateResponse(Long id, ActionResponseRequest request);
    void deleteResponse(Long id);
}

