package com.rssb.application.service;

import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;

import java.util.List;

/**
 * Service interface for Sewadar operations.
 */
public interface SewadarService {
    List<SewadarResponse> getAllSewadars();
    SewadarResponse getSewadarById(String zonalId); // Changed to String
    SewadarResponse createSewadar(SewadarRequest request);
    SewadarResponse updateSewadar(String zonalId, SewadarRequest request); // Changed to String
    void deleteSewadar(String zonalId); // Changed to String
    SewadarResponse promoteToIncharge(String sewadarZonalId, String inchargeZonalId, String password); // Changed to String
    SewadarResponse demoteToSewadar(String sewadarZonalId, String inchargeZonalId, String password); // Changed to String
}

