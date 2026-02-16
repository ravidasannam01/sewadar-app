package com.rssb.application.controller;

import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.service.SewadarService;
import com.rssb.application.util.ActionLogger;
import com.rssb.application.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Sewadar operations.
 */
@RestController
@RequestMapping("/api/sewadars")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("unused") // REST endpoints are used via HTTP, not direct Java calls
public class SewadarController {

    private final SewadarService sewadarService;
    private final ActionLogger actionLogger;

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
     * Get a sewadar by zonal ID.
     *
     * @param id The sewadar zonal ID
     * @return The sewadar response
     */
    @GetMapping("/{id}")
    public ResponseEntity<SewadarResponse> getSewadarById(@PathVariable String id) {
        log.info("GET /api/sewadars/{} - Fetching sewadar by zonal_id", id);
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
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        SewadarResponse createdSewadar = sewadarService.createSewadar(request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("sewadarZonalId", createdSewadar.getZonalId());
        details.put("name", createdSewadar.getFirstName() + " " + createdSewadar.getLastName());
        details.put("role", createdSewadar.getRole());
        details.put("location", createdSewadar.getLocation());
        details.put("mobile", createdSewadar.getMobile());
        details.put("durationMs", duration);
        actionLogger.logAction("CREATE_SEWADAR", userId, userRole, details);
        actionLogger.logPerformance("CREATE_SEWADAR", duration);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSewadar);
    }

    /**
     * Update an existing sewadar.
     *
     * @param id      The sewadar zonal ID
     * @param request The sewadar request DTO
     * @return The updated sewadar response
     */
    @PutMapping("/{id}")
    public ResponseEntity<SewadarResponse> updateSewadar(
            @PathVariable String id,
            @Valid @RequestBody SewadarRequest request) {
        log.info("PUT /api/sewadars/{} - Updating sewadar", id);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        SewadarResponse updatedSewadar = sewadarService.updateSewadar(id, request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("sewadarZonalId", id);
        details.put("name", updatedSewadar.getFirstName() + " " + updatedSewadar.getLastName());
        details.put("durationMs", duration);
        actionLogger.logAction("UPDATE_SEWADAR", userId, userRole, details);
        actionLogger.logPerformance("UPDATE_SEWADAR", duration);
        
        return ResponseEntity.ok(updatedSewadar);
    }

    /**
     * Delete a sewadar by zonal ID.
     *
     * @param id The sewadar zonal ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSewadar(@PathVariable String id) {
        log.info("DELETE /api/sewadars/{} - Deleting sewadar", id);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        // Get sewadar details before deletion for logging
        try {
            SewadarResponse sewadar = sewadarService.getSewadarById(id);
            Map<String, Object> details = new HashMap<>();
            details.put("sewadarZonalId", id);
            details.put("name", sewadar.getFirstName() + " " + sewadar.getLastName());
            actionLogger.logAction("DELETE_SEWADAR", userId, userRole, details);
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("sewadarZonalId", id);
            actionLogger.logAction("DELETE_SEWADAR", userId, userRole, details);
        }
        
        sewadarService.deleteSewadar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Promote a sewadar to incharge role.
     * Only existing incharge can perform this action.
     * Requires incharge password verification.
     *
     * @param sewadarId The sewadar zonal ID to promote
     * @param inchargeId The incharge zonal ID performing the promotion
     * @param password The incharge password for verification
     * @return The updated sewadar response
     */
    @PostMapping("/{sewadarId}/promote")
    public ResponseEntity<SewadarResponse> promoteToIncharge(
            @PathVariable String sewadarId,
            @RequestParam String inchargeId,
            @RequestParam String password) {
        log.info("POST /api/sewadars/{}/promote - Promoting to incharge", sewadarId);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        SewadarResponse updated = sewadarService.promoteToIncharge(sewadarId, inchargeId, password);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("sewadarZonalId", sewadarId);
        details.put("newRole", updated.getRole());
        details.put("name", updated.getFirstName() + " " + updated.getLastName());
        details.put("promotedBy", inchargeId);
        details.put("durationMs", duration);
        actionLogger.logAction("PROMOTE_TO_INCHARGE", userId != null ? userId : inchargeId, userRole, details);
        actionLogger.logPerformance("PROMOTE_TO_INCHARGE", duration);
        
        return ResponseEntity.ok(updated);
    }

    /**
     * Demote an incharge to sewadar role.
     * Only existing incharge can perform this action.
     * Requires incharge password verification.
     * Cannot demote yourself.
     *
     * @param sewadarId The incharge zonal ID to demote
     * @param inchargeId The incharge zonal ID performing the demotion
     * @param password The incharge password for verification
     * @return The updated sewadar response
     */
    @PostMapping("/{sewadarId}/demote")
    public ResponseEntity<SewadarResponse> demoteToSewadar(
            @PathVariable String sewadarId,
            @RequestParam String inchargeId,
            @RequestParam String password) {
        log.info("POST /api/sewadars/{}/demote - Demoting to sewadar", sewadarId);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        SewadarResponse updated = sewadarService.demoteToSewadar(sewadarId, inchargeId, password);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("sewadarZonalId", sewadarId);
        details.put("newRole", updated.getRole());
        details.put("name", updated.getFirstName() + " " + updated.getLastName());
        details.put("demotedBy", inchargeId);
        details.put("durationMs", duration);
        actionLogger.logAction("DEMOTE_TO_SEWADAR", userId != null ? userId : inchargeId, userRole, details);
        actionLogger.logPerformance("DEMOTE_TO_SEWADAR", duration);
        
        return ResponseEntity.ok(updated);
    }
}

