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
            @PathVariable String inchargeId) {
        return ResponseEntity.ok(workflowService.getWorkflowsForIncharge(inchargeId));
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
    public ResponseEntity<Void> notifyMissingFormSubmissions(
            @PathVariable Long programId) {
        workflowService.notifyMissingFormSubmitters(programId);
        return ResponseEntity.ok().build();
    }
}
