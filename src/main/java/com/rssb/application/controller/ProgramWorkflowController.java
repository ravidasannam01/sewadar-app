package com.rssb.application.controller;

import com.rssb.application.dto.ProgramWorkflowResponse;
import com.rssb.application.service.ProgramWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgramWorkflowController {

    private final ProgramWorkflowService workflowService;

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
        
        return ResponseEntity.ok(workflowService.moveToNextNode(programId, inchargeId));
    }

    @PostMapping("/program/{programId}/release-form")
    public ResponseEntity<ProgramWorkflowResponse> releaseForm(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        
        return ResponseEntity.ok(workflowService.releaseForm(programId, inchargeId));
    }

    @PostMapping("/program/{programId}/mark-details-collected")
    public ResponseEntity<ProgramWorkflowResponse> markDetailsCollected(
            @PathVariable Long programId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String inchargeId = (String) auth.getPrincipal();
        
        return ResponseEntity.ok(workflowService.markDetailsCollected(programId, inchargeId));
    }

    @PostMapping("/initialize-all")
    public ResponseEntity<String> initializeAllMissingWorkflows() {
        int count = workflowService.initializeAllMissingWorkflows();
        return ResponseEntity.ok("Initialized " + count + " missing workflows");
    }
}
