package com.rssb.application.controller;

import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.service.SewadarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Sewadar operations.
 */
@RestController
@RequestMapping("/api/sewadars")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SewadarController {

    private final SewadarService sewadarService;

    /**
     * Get all sewadars.
     *
     * @return List of all sewadars
     */
    @GetMapping
    public ResponseEntity<List<SewadarResponse>> getAllSewadars() {
        log.info("GET /api/sewadars - Fetching all sewadars");
        List<SewadarResponse> sewadars = sewadarService.getAllSewadars();
        return ResponseEntity.ok(sewadars);
    }

    /**
     * Get a sewadar by ID.
     *
     * @param id The sewadar ID
     * @return The sewadar response
     */
    @GetMapping("/{id}")
    public ResponseEntity<SewadarResponse> getSewadarById(@PathVariable Long id) {
        log.info("GET /api/sewadars/{} - Fetching sewadar by id", id);
        SewadarResponse sewadar = sewadarService.getSewadarById(id);
        return ResponseEntity.ok(sewadar);
    }

    /**
     * Create a new sewadar.
     *
     * @param request The sewadar request DTO
     * @return The created sewadar response
     */
    @PostMapping
    public ResponseEntity<SewadarResponse> createSewadar(@Valid @RequestBody SewadarRequest request) {
        log.info("POST /api/sewadars - Creating new sewadar");
        SewadarResponse createdSewadar = sewadarService.createSewadar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSewadar);
    }

    /**
     * Update an existing sewadar.
     *
     * @param id      The sewadar ID
     * @param request The sewadar request DTO
     * @return The updated sewadar response
     */
    @PutMapping("/{id}")
    public ResponseEntity<SewadarResponse> updateSewadar(
            @PathVariable Long id,
            @Valid @RequestBody SewadarRequest request) {
        log.info("PUT /api/sewadars/{} - Updating sewadar", id);
        SewadarResponse updatedSewadar = sewadarService.updateSewadar(id, request);
        return ResponseEntity.ok(updatedSewadar);
    }

    /**
     * Delete a sewadar by ID.
     *
     * @param id The sewadar ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSewadar(@PathVariable Long id) {
        log.info("DELETE /api/sewadars/{} - Deleting sewadar", id);
        sewadarService.deleteSewadar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Promote a sewadar to incharge role.
     * Only existing incharge can perform this action.
     *
     * @param sewadarId The sewadar ID to promote
     * @param inchargeId The incharge ID performing the promotion
     * @return The updated sewadar response
     */
    @PostMapping("/{sewadarId}/promote")
    public ResponseEntity<SewadarResponse> promoteToIncharge(
            @PathVariable Long sewadarId,
            @RequestParam Long inchargeId) {
        log.info("POST /api/sewadars/{}/promote - Promoting to incharge", sewadarId);
        SewadarResponse updated = sewadarService.promoteToIncharge(sewadarId, inchargeId);
        return ResponseEntity.ok(updated);
    }
}

