package com.rssb.application.service;

import com.rssb.application.dto.ProgramSelectionRequest;
import com.rssb.application.dto.ProgramSelectionResponse;
import com.rssb.application.dto.SewadarResponse;

import java.util.List;

public interface ProgramSelectionService {
    List<ProgramSelectionResponse> selectSewadars(ProgramSelectionRequest request);
    List<ProgramSelectionResponse> getSelectionsByProgram(Long programId);
    List<ProgramSelectionResponse> getSelectionsBySewadar(Long sewadarId);
    List<SewadarResponse> getPrioritizedSewadars(Long programId, String sortBy);
    ProgramSelectionResponse updateSelectionStatus(Long id, String status);
    void removeSelection(Long id);
}

