package com.rssb.application.controller;

import com.rssb.application.dto.ProgramApplicationRequest;
import com.rssb.application.dto.ProgramApplicationResponse;
import com.rssb.application.service.ProgramApplicationService;
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

@RestController
@RequestMapping("/api/program-applications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("unused") // REST endpoints are used via HTTP, not direct Java calls
public class ProgramApplicationController {

    private final ProgramApplicationService applicationService;
    private final ActionLogger actionLogger;

    @PostMapping
    public ResponseEntity<ProgramApplicationResponse> applyToProgram(@Valid @RequestBody ProgramApplicationRequest request) {
        log.info("POST /api/program-applications - Applying to program");
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramApplicationResponse response = applicationService.applyToProgram(request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", response.getId());
        details.put("programId", request.getProgramId());
        details.put("sewadarId", request.getSewadarId());
        details.put("status", response.getStatus());
        details.put("durationMs", duration);
        actionLogger.logAction("APPLY_TO_PROGRAM", userId != null ? userId : request.getSewadarId(), userRole, details);
        actionLogger.logPerformance("APPLY_TO_PROGRAM", duration);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
    public ResponseEntity<List<ProgramApplicationResponse>> getApplicationsBySewadar(@PathVariable String sewadarId) {
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
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramApplicationResponse response = applicationService.updateApplicationStatus(id, status);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", id);
        details.put("newStatus", status);
        details.put("sewadarId", response.getSewadar() != null ? response.getSewadar().getZonalId() : "N/A");
        details.put("programId", response.getProgramId());
        details.put("durationMs", duration);
        
        String action = "APPROVE_APPLICATION".equals(status) ? "APPROVE_APPLICATION" : 
                        "REJECT_APPLICATION".equals(status) ? "REJECT_APPLICATION" : 
                        "UPDATE_APPLICATION_STATUS";
        actionLogger.logAction(action, userId, userRole, details);
        actionLogger.logPerformance(action, duration);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Sewadar requests to drop from a program (requires incharge approval)
     * @param id Application ID
     * @param sewadarId Sewadar zonal ID (must own the application)
     */
    @PutMapping("/{id}/request-drop")
    public ResponseEntity<ProgramApplicationResponse> requestDrop(
            @PathVariable Long id,
            @RequestParam String sewadarId) {
        log.info("PUT /api/program-applications/{}/request-drop", id);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramApplicationResponse response = applicationService.requestDrop(id, sewadarId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", id);
        details.put("sewadarId", sewadarId);
        details.put("programId", response.getProgramId());
        details.put("durationMs", duration);
        actionLogger.logAction("REQUEST_DROP", userId != null ? userId : sewadarId, userRole, details);
        actionLogger.logPerformance("REQUEST_DROP", duration);
        
        return ResponseEntity.ok(response);
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
            @RequestParam String inchargeId,
            @RequestParam(required = false, defaultValue = "true") Boolean allowReapply) {
        log.info("PUT /api/program-applications/{}/approve-drop - incharge: {}", id, inchargeId);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramApplicationResponse response = applicationService.approveDropRequest(id, inchargeId, allowReapply);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", id);
        details.put("inchargeId", inchargeId);
        details.put("sewadarId", response.getSewadar() != null ? response.getSewadar().getZonalId() : "N/A");
        details.put("programId", response.getProgramId());
        details.put("allowReapply", allowReapply);
        details.put("durationMs", duration);
        actionLogger.logAction("APPROVE_DROP_REQUEST", userId != null ? userId : inchargeId, userRole, details);
        actionLogger.logPerformance("APPROVE_DROP_REQUEST", duration);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get drop requests for a program (for incharge)
     */
    @GetMapping("/program/{programId}/drop-requests")
    public ResponseEntity<List<ProgramApplicationResponse>> getDropRequests(@PathVariable Long programId) {
        log.info("GET /api/program-applications/program/{}/drop-requests", programId);
        return ResponseEntity.ok(applicationService.getDropRequestsByProgram(programId));
    }

    /**
     * Rollback an application from APPROVED or REJECTED to PENDING
     * Only the program creator (incharge) can rollback applications
     * @param id Application ID
     * @param inchargeId Incharge zonal ID (must be program creator)
     */
    @PutMapping("/{id}/rollback")
    public ResponseEntity<ProgramApplicationResponse> rollbackApplication(
            @PathVariable Long id,
            @RequestParam String inchargeId) {
        log.info("PUT /api/program-applications/{}/rollback - incharge: {}", id, inchargeId);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramApplicationResponse response = applicationService.rollbackApplication(id, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("applicationId", id);
        details.put("inchargeId", inchargeId);
        details.put("sewadarId", response.getSewadar() != null ? response.getSewadar().getZonalId() : "N/A");
        details.put("programId", response.getProgramId());
        details.put("newStatus", response.getStatus());
        details.put("durationMs", duration);
        actionLogger.logAction("ROLLBACK_APPLICATION", userId != null ? userId : inchargeId, userRole, details);
        actionLogger.logPerformance("ROLLBACK_APPLICATION", duration);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable Long id) {
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }
}

