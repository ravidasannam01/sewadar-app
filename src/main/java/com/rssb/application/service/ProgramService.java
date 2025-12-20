package com.rssb.application.service;

import com.rssb.application.dto.ProgramRequest;
import com.rssb.application.dto.ProgramResponse;

import java.util.List;

public interface ProgramService {
    ProgramResponse createProgram(ProgramRequest request);
    ProgramResponse getProgramById(Long id);
    List<ProgramResponse> getAllPrograms();
    List<ProgramResponse> getProgramsByIncharge(Long inchargeId);
    ProgramResponse updateProgram(Long id, ProgramRequest request);
    void deleteProgram(Long id);
}

