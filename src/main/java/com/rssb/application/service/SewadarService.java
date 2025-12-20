package com.rssb.application.service;

import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;

import java.util.List;

/**
 * Service interface for Sewadar operations.
 */
public interface SewadarService {
    List<SewadarResponse> getAllSewadars();
    SewadarResponse getSewadarById(Long id);
    SewadarResponse createSewadar(SewadarRequest request);
    SewadarResponse updateSewadar(Long id, SewadarRequest request);
    void deleteSewadar(Long id);
}

