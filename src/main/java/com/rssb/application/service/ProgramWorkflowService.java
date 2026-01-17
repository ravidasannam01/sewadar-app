package com.rssb.application.service;

import com.rssb.application.dto.ProgramWorkflowResponse;
import com.rssb.application.entity.*;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing program workflow.
 * Handles sequential workflow nodes and state transitions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgramWorkflowService {

    private final ProgramWorkflowRepository workflowRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final com.rssb.application.repository.ProgramNotificationPreferenceRepository programNotificationPreferenceRepository;
    private final WhatsAppService whatsAppService;
    
    // Self-injection for transaction proxy
    private ProgramWorkflowService self;
    
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    public void setSelf(ProgramWorkflowService self) {
        this.self = self;
    }

    // Workflow node definitions
    private static final String[] NODE_NAMES = {
        "Make Program Active",
        "Post Application Message",
        "Release Form",
        "Collect Details",
        "Post Mail to Area Secretary",
        "Post General Instructions"
    };

    // Node messages are stored in NotificationPreference entity

    /**
     * Initialize workflow for a new program.
     */
    @Transactional
    public ProgramWorkflow initializeWorkflow(Program program) {
        return workflowRepository.findByProgramId(program.getId())
            .orElseGet(() -> {
                ProgramWorkflow workflow = ProgramWorkflow.builder()
                    .program(program)
                    .currentNode(1)
                    .formReleased(false)
                    .detailsCollected(false)
                    .build();
                return workflowRepository.save(workflow);
            });
    }

    /**
     * Get workflow for a program.
     */
    public ProgramWorkflowResponse getWorkflow(Long programId) {
        try {
            log.debug("Getting workflow for program: {}", programId);
            
            Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

            Optional<ProgramWorkflow> workflowOpt = workflowRepository.findByProgramId(programId);
            
            if (workflowOpt.isEmpty()) {
                // Initialize in a separate write transaction using self-injection
                log.info("Workflow not found for program {}, initializing...", programId);
                if (self != null) {
                    return self.initializeAndGetWorkflow(programId, program);
                } else {
                    // Fallback: use direct initialization
                    log.warn("Self-injection not available, initializing workflow directly");
                    ProgramWorkflow workflow = initializeWorkflow(program);
                    return mapToResponse(workflow, program);
                }
            }

            ProgramWorkflow workflow = workflowOpt.get();
            // Access program data within transaction to avoid lazy loading issues
            return mapToResponse(workflow, program);
        } catch (ResourceNotFoundException e) {
            log.error("Program not found: {}", programId);
            throw e;
        } catch (Exception e) {
            log.error("Error getting workflow for program {}: {}", programId, e.getMessage(), e);
            throw new RuntimeException("Failed to get workflow for program " + programId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Initialize workflow in a separate write transaction.
     */
    @Transactional
    public ProgramWorkflowResponse initializeAndGetWorkflow(Long programId, Program program) {
        ProgramWorkflow workflow = initializeWorkflow(program);
        return mapToResponse(workflow, program);
    }

    /**
     * Get all workflows for programs created by an incharge.
     * Automatically initializes workflows for programs that don't have one.
     */
    @Transactional(readOnly = true)
    public List<ProgramWorkflowResponse> getWorkflowsForIncharge(Long inchargeId) {
        List<Program> programs = programRepository.findByCreatedByZonalId(inchargeId);
        return programs.stream()
            .map(program -> {
                try {
                    Optional<ProgramWorkflow> workflowOpt = workflowRepository.findByProgramId(program.getId());
                    
                    if (workflowOpt.isEmpty()) {
                        // Initialize in a separate write transaction using self-injection
                        log.info("Initializing workflow for existing program: {}", program.getId());
                        if (self != null) {
                            return self.initializeAndGetWorkflow(program.getId(), program);
                        } else {
                            // Fallback
                            ProgramWorkflow workflow = initializeWorkflow(program);
                            return mapToResponse(workflow, program);
                        }
                    }
                    
                    ProgramWorkflow workflow = workflowOpt.get();
                    // Access program data within transaction to avoid lazy loading issues
                    return mapToResponse(workflow, program);
                } catch (Exception e) {
                    log.error("Error getting workflow for program {}", program.getId(), e);
                    throw new RuntimeException("Failed to get workflow for program " + program.getId(), e);
                }
            })
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Initialize workflows for all existing programs that don't have one.
     * Useful for migration or fixing missing workflows.
     */
    @Transactional
    public int initializeAllMissingWorkflows() {
        List<Program> allPrograms = programRepository.findAll();
        int initialized = 0;
        
        for (Program program : allPrograms) {
            if (!workflowRepository.findByProgramId(program.getId()).isPresent()) {
                initializeWorkflow(program);
                initialized++;
                log.info("Initialized workflow for program: {}", program.getId());
            }
        }
        
        return initialized;
    }

    /**
     * Move to next workflow node.
     */
    public ProgramWorkflowResponse moveToNextNode(Long programId, Long inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() < 6) {
            workflow.setCurrentNode(workflow.getCurrentNode() + 1);
            workflow = workflowRepository.save(workflow);
            log.info("Workflow moved to node {} for program {}", workflow.getCurrentNode(), programId);
        }

        return mapToResponse(workflow, program);
    }

    /**
     * Release form for sewadars.
     */
    public ProgramWorkflowResponse releaseForm(Long programId, Long inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() == 3) {
            workflow.setFormReleased(true);
            workflow.setCurrentNode(4);
            workflow = workflowRepository.save(workflow);
            log.info("Form released for program {}", programId);
        }

        return mapToResponse(workflow, program);
    }

    /**
     * Mark details as collected.
     */
    public ProgramWorkflowResponse markDetailsCollected(Long programId, Long inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() == 4) {
            workflow.setDetailsCollected(true);
            workflow.setCurrentNode(5);
            workflow = workflowRepository.save(workflow);
            log.info("Details collected for program {}", programId);
        }

        return mapToResponse(workflow, program);
    }

    /**
     * Send daily notifications for programs at current workflow node.
     * Called by scheduler.
     */
    public void sendDailyNotifications() {
        List<ProgramWorkflow> workflows = workflowRepository.findAll();
        
        for (ProgramWorkflow workflow : workflows) {
            try {
                // Use program ID from workflow's program reference, but fetch fresh to avoid lazy loading
                Long programId = workflow.getProgram() != null ? workflow.getProgram().getId() : null;
                if (programId == null) {
                    continue;
                }
                Program program = programRepository.findById(programId)
                    .orElse(null);
                
                if (program == null) {
                    continue;
                }
                
                Integer currentNode = workflow.getCurrentNode();
                
                // Check notification preference: program-level overrides global
                Boolean notificationEnabled = isNotificationEnabled(program, currentNode);
                
                if (notificationEnabled == null || !notificationEnabled) {
                    continue;
                }

                // Get global notification message
                NotificationPreference globalPreference = notificationPreferenceRepository
                    .findByNodeNumber(currentNode)
                    .orElse(null);
                
                if (globalPreference == null) {
                    continue;
                }

                // Get incharge(s) - all incharges for now
                List<Sewadar> incharges = sewadarRepository.findByRole(Role.INCHARGE);
                
                String message = globalPreference.getNotificationMessage()
                    .replace("{programTitle}", program.getTitle());
                
                for (Sewadar incharge : incharges) {
                    if (incharge.getMobile() != null && !incharge.getMobile().isEmpty()) {
                        whatsAppService.sendMessage(incharge.getMobile(), message);
                        log.info("Sent notification to incharge {} for program {} at node {}", 
                            incharge.getZonalId(), program.getId(), currentNode);
                    }
                }
            } catch (Exception e) {
                log.error("Error sending notification for workflow {}", workflow.getId(), e);
            }
        }
    }

    /**
     * Check if notification is enabled for a program at a specific node.
     * Returns: program-level setting if exists, otherwise global setting.
     */
    private Boolean isNotificationEnabled(Program program, Integer nodeNumber) {
        // Check program-level preference first
        com.rssb.application.entity.ProgramNotificationPreference programPref = 
            programNotificationPreferenceRepository
                .findByProgramAndNodeNumber(program, nodeNumber)
                .orElse(null);
        
        if (programPref != null && programPref.getEnabled() != null) {
            return programPref.getEnabled();
        }
        
        // Fall back to global preference
        NotificationPreference globalPref = notificationPreferenceRepository
            .findByNodeNumber(nodeNumber)
            .orElse(null);
        
        return globalPref != null ? globalPref.getEnabled() : false;
    }

    /**
     * Auto-advance workflow based on program state.
     */
    public void checkAndAdvanceWorkflow(Program program) {
        ProgramWorkflow workflow = workflowRepository.findByProgramId(program.getId())
            .orElseGet(() -> initializeWorkflow(program));

        // Node 1: Program becomes active
        if (workflow.getCurrentNode() == 1 && "active".equals(program.getStatus())) {
            workflow.setCurrentNode(2);
            workflowRepository.save(workflow);
            log.info("Workflow auto-advanced to node 2 for program {}", program.getId());
        }

        // Node 2: Check if applications are full
        if (workflow.getCurrentNode() == 2 && program.getMaxSewadars() != null) {
            long applicationCount = program.getApplications() != null 
                ? program.getApplications().stream()
                    .filter(app -> "APPROVED".equals(app.getStatus()))
                    .count()
                : 0;
            
            if (applicationCount >= program.getMaxSewadars()) {
                workflow.setCurrentNode(3);
                workflowRepository.save(workflow);
                log.info("Workflow auto-advanced to node 3 for program {} (applications full)", program.getId());
            }
        }
    }

    private ProgramWorkflowResponse mapToResponse(ProgramWorkflow workflow, Program program) {
        Integer node = workflow.getCurrentNode();
        return ProgramWorkflowResponse.builder()
            .id(workflow.getId())
            .programId(program.getId())
            .programTitle(program.getTitle())
            .currentNode(node)
            .currentNodeName(node <= NODE_NAMES.length ? NODE_NAMES[node - 1] : "Unknown")
            .formReleased(workflow.getFormReleased())
            .detailsCollected(workflow.getDetailsCollected())
            .build();
    }
}

