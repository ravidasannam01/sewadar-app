package com.rssb.application.service;

import com.rssb.application.dto.ProgramApplicationRequest;
import com.rssb.application.dto.ProgramApplicationResponse;

import java.util.List;

public interface ProgramApplicationService {
    ProgramApplicationResponse applyToProgram(ProgramApplicationRequest request);
    List<ProgramApplicationResponse> getApplicationsByProgram(Long programId);
    List<ProgramApplicationResponse> getApplicationsBySewadar(Long sewadarId);
    ProgramApplicationResponse updateApplicationStatus(Long id, String status);
    void deleteApplication(Long id);
    List<com.rssb.application.dto.PrioritizedApplicationResponse> getPrioritizedApplications(
            Long programId, String sortBy, String order);
}

