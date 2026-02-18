package com.rssb.application.controller;

import com.rssb.application.dto.ProgramWorkflowResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.service.ProgramWorkflowService;
import com.rssb.application.util.ActionLogger;
import com.rssb.application.util.UserContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgramWorkflowController {

    private final ProgramWorkflowService workflowService;
    private final ActionLogger actionLogger;

    @GetMapping("/program/{programId}")
    public ResponseEntity<ProgramWorkflowResponse> getWorkflow(@PathVariable Long programId) {
        try {
            log.info("Getting workflow for program: {}", programId);
            return ResponseEntity.ok(workflowService.getWorkflow(programId));
        } catch (Exception e) {
            log.error("Error getting workflow for program {}", programId, e);
            throw e;
        }
    }

    @GetMapping("/incharge/{inchargeId}")
    public ResponseEntity<List<ProgramWorkflowResponse>> getWorkflowsForIncharge(
            @PathVariable String inchargeId,
            @RequestParam(required = false) Boolean archived) {
        return ResponseEntity.ok(workflowService.getWorkflowsForIncharge(inchargeId, archived));
    }

    @PostMapping("/program/{programId}/next-node")
    public ResponseEntity<ProgramWorkflowResponse> moveToNextNode(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramWorkflowResponse response = workflowService.moveToNextNode(programId, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("currentNode", response.getCurrentNode());
        details.put("durationMs", duration);
        actionLogger.logAction("MOVE_WORKFLOW_NEXT_NODE", inchargeId, userRole, details);
        actionLogger.logPerformance("MOVE_WORKFLOW_NEXT_NODE", duration);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/program/{programId}/release-form")
    public ResponseEntity<ProgramWorkflowResponse> releaseForm(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramWorkflowResponse response = workflowService.releaseForm(programId, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("formReleased", response.getFormReleased());
        details.put("currentNode", response.getCurrentNode());
        details.put("durationMs", duration);
        actionLogger.logAction("RELEASE_WORKFLOW_FORM", inchargeId, userRole, details);
        actionLogger.logPerformance("RELEASE_WORKFLOW_FORM", duration);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/program/{programId}/mark-details-collected")
    public ResponseEntity<ProgramWorkflowResponse> markDetailsCollected(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramWorkflowResponse response = workflowService.markDetailsCollected(programId, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("detailsCollected", response.getDetailsCollected());
        details.put("currentNode", response.getCurrentNode());
        details.put("durationMs", duration);
        actionLogger.logAction("MARK_WORKFLOW_DETAILS_COLLECTED", inchargeId, userRole, details);
        actionLogger.logPerformance("MARK_WORKFLOW_DETAILS_COLLECTED", duration);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/initialize-all")
    public ResponseEntity<String> initializeAllMissingWorkflows() {
        int count = workflowService.initializeAllMissingWorkflows();
        return ResponseEntity.ok("Initialized " + count + " missing workflows");
    }

    /**
     * Get approved sewadars who have NOT yet submitted forms for this program.
     */
    @GetMapping("/program/{programId}/missing-forms")
    public ResponseEntity<List<SewadarResponse>> getMissingFormSubmissions(
            @PathVariable Long programId) {
        return ResponseEntity.ok(workflowService.getMissingFormSubmitters(programId));
    }

    /**
     * Notify all approved sewadars who have not submitted forms yet via WhatsApp.
     */
    @PostMapping("/program/{programId}/notify-missing-forms")
    public ResponseEntity<Map<String, Object>> notifyMissingFormSubmissions(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        workflowService.notifyMissingFormSubmitters(programId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("durationMs", duration);
        actionLogger.logAction("NOTIFY_MISSING_FORMS", inchargeId, userRole, details);
        actionLogger.logPerformance("NOTIFY_MISSING_FORMS", duration);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notifications sent to missing form submitters");
        return ResponseEntity.ok(response);
    }

    /**
     * Archive a workflow (mark as complete and archived).
     * Only workflows at node 6 can be archived.
     */
    @PostMapping("/program/{programId}/archive")
    public ResponseEntity<ProgramWorkflowResponse> archiveWorkflow(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramWorkflowResponse response = workflowService.archiveWorkflow(programId, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("archived", true);
        details.put("durationMs", duration);
        actionLogger.logAction("ARCHIVE_WORKFLOW", inchargeId, userRole, details);
        actionLogger.logPerformance("ARCHIVE_WORKFLOW", duration);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Unarchive a workflow (restore from archive).
     */
    @PostMapping("/program/{programId}/unarchive")
    public ResponseEntity<ProgramWorkflowResponse> unarchiveWorkflow(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramWorkflowResponse response = workflowService.unarchiveWorkflow(programId, inchargeId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", programId);
        details.put("archived", false);
        details.put("durationMs", duration);
        actionLogger.logAction("UNARCHIVE_WORKFLOW", inchargeId, userRole, details);
        actionLogger.logPerformance("UNARCHIVE_WORKFLOW", duration);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger workflow notifications for all programs.
     * This is the same as the daily scheduler (runs at 9:00 AM) but can be triggered on-demand.
     * Only accessible to INCHARGE and ADMIN.
     */
    @PostMapping("/trigger-notifications")
    public ResponseEntity<Map<String, Object>> triggerNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (String) auth.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        log.info("Manual notification trigger requested by user: {} ({})", userId, userRole);
        
        long startTime = System.currentTimeMillis();
        try {
            workflowService.sendDailyNotifications();
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> details = new HashMap<>();
            details.put("triggeredBy", userId);
            details.put("durationMs", duration);
            actionLogger.logAction("MANUAL_TRIGGER_NOTIFICATIONS", userId, userRole, details);
            actionLogger.logPerformance("MANUAL_TRIGGER_NOTIFICATIONS", duration);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow notifications triggered successfully for all programs");
            response.put("durationMs", duration);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering workflow notifications manually", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to trigger notifications: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
