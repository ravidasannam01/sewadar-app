package com.rssb.application.controller;

import com.rssb.application.dto.ProgramApplicationRequest;
import com.rssb.application.dto.ProgramApplicationResponse;
import com.rssb.application.service.ProgramApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-applications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgramApplicationController {

    private final ProgramApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ProgramApplicationResponse> applyToProgram(@Valid @RequestBody ProgramApplicationRequest request) {
        log.info("POST /api/program-applications - Applying to program");
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.applyToProgram(request));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<ProgramApplicationResponse>> getApplicationsByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(applicationService.getApplicationsByProgram(programId));
    }

    /**
     * Get applications by sewadar zonal ID
     * @param sewadarId Sewadar zonal ID
     * @return List of applications
     */
    @GetMapping("/sewadar/{sewadarId}")
    public ResponseEntity<List<ProgramApplicationResponse>> getApplicationsBySewadar(@PathVariable Long sewadarId) {
        return ResponseEntity.ok(applicationService.getApplicationsBySewadar(sewadarId));
    }

    /**
     * Get prioritized applications for a program with sorting options
     * @param programId Program ID
     * @param sortBy Sort by: attendance, beasAttendance, nonBeasAttendance, days, beasDays, nonBeasDays, profession, joiningDate, priorityScore
     * @param order Sort order: asc or desc (default: desc)
     */
    @GetMapping("/program/{programId}/prioritized")
    public ResponseEntity<List<com.rssb.application.dto.PrioritizedApplicationResponse>> getPrioritizedApplications(
            @PathVariable Long programId,
            @RequestParam(required = false, defaultValue = "priorityScore") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String order) {
        log.info("GET /api/program-applications/program/{}/prioritized - sortBy: {}, order: {}", programId, sortBy, order);
        return ResponseEntity.ok(applicationService.getPrioritizedApplications(programId, sortBy, order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProgramApplicationResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(applicationService.updateApplicationStatus(id, status));
    }

    /**
     * Sewadar requests to drop from a program (requires incharge approval)
     * @param id Application ID
     * @param sewadarId Sewadar zonal ID (must own the application)
     */
    @PutMapping("/{id}/request-drop")
    public ResponseEntity<ProgramApplicationResponse> requestDrop(
            @PathVariable Long id,
            @RequestParam Long sewadarId) {
        log.info("PUT /api/program-applications/{}/request-drop", id);
        return ResponseEntity.ok(applicationService.requestDrop(id, sewadarId));
    }

    /**
     * Incharge approves drop request
     * Note: Reapply is always allowed (reapply_allowed field removed from schema)
     * @param id Application ID
     * @param inchargeId Incharge zonal ID (must be program creator)
     * @param allowReapply Deprecated - kept for backward compatibility, always true
     */
    @PutMapping("/{id}/approve-drop")
    public ResponseEntity<ProgramApplicationResponse> approveDropRequest(
            @PathVariable Long id,
            @RequestParam Long inchargeId,
            @RequestParam(required = false, defaultValue = "true") Boolean allowReapply) {
        log.info("PUT /api/program-applications/{}/approve-drop - incharge: {}", id, inchargeId);
        return ResponseEntity.ok(applicationService.approveDropRequest(id, inchargeId, allowReapply));
    }

    /**
     * Get drop requests for a program (for incharge)
     */
    @GetMapping("/program/{programId}/drop-requests")
    public ResponseEntity<List<ProgramApplicationResponse>> getDropRequests(@PathVariable Long programId) {
        log.info("GET /api/program-applications/program/{}/drop-requests", programId);
        return ResponseEntity.ok(applicationService.getDropRequestsByProgram(programId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}

