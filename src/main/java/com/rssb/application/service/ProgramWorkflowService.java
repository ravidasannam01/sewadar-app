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
import java.util.stream.Collectors;

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
    private final ProgramApplicationRepository applicationRepository;
    private final SewadarFormSubmissionRepository formSubmissionRepository;
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
     * Get all workflows for programs.
     * ADMIN sees all programs, INCHARGE sees only programs they created.
     * Automatically initializes workflows for programs that don't have one.
     */
    @Transactional(readOnly = true)
    public List<ProgramWorkflowResponse> getWorkflowsForIncharge(String inchargeId) {
        // Check if user is ADMIN - if so, return all programs
        Sewadar user = sewadarRepository.findByZonalId(inchargeId).orElse(null);
        List<Program> programs;
        if (user != null && user.getRole() == com.rssb.application.entity.Role.ADMIN) {
            programs = programRepository.findAll();
        } else {
            programs = programRepository.findByCreatedByZonalId(inchargeId);
        }
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
    public ProgramWorkflowResponse moveToNextNode(Long programId, String inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() < 6) {
            workflow.setCurrentNode(workflow.getCurrentNode() + 1);
            workflow = workflowRepository.save(workflow);
            log.info("Workflow moved to node {} for program {}", workflow.getCurrentNode(), programId);

            // Send notifications immediately for the new node
            sendNotificationsForNode(program, workflow);
        }

        return mapToResponse(workflow, program);
    }

    /**
     * Release form for sewadars.
     */
    public ProgramWorkflowResponse releaseForm(Long programId, String inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() == 3) {
            workflow.setFormReleased(true);
            workflow.setCurrentNode(4);
            workflow = workflowRepository.save(workflow);
            log.info("Form released for program {}", programId);

            // Send notifications immediately for node 4
            sendNotificationsForNode(program, workflow);
        }

        return mapToResponse(workflow, program);
    }

    /**
     * Mark details as collected.
     */
    public ProgramWorkflowResponse markDetailsCollected(Long programId, String inchargeId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramWorkflow workflow = workflowRepository.findByProgramId(programId)
            .orElseGet(() -> initializeWorkflow(program));

        if (workflow.getCurrentNode() != 4) {
            return mapToResponse(workflow, program);
        }

        // Before moving to next step, ensure ALL approved sewadars have submitted forms
        List<ProgramApplication> approvedApps = applicationRepository
                .findByProgramIdAndStatus(programId, "APPROVED");

        List<Sewadar> missing = new java.util.ArrayList<>();
        for (ProgramApplication app : approvedApps) {
            boolean hasSubmission = formSubmissionRepository
                    .findByProgramIdAndSewadarZonalId(programId, app.getSewadar().getZonalId())
                    .isPresent();
            if (!hasSubmission) {
                missing.add(app.getSewadar());
            }
        }

        if (!missing.isEmpty()) {
            // Build a simple message for logs and frontend
            String missingIds = missing.stream()
                    .map(Sewadar::getZonalId)
                    .collect(java.util.stream.Collectors.joining(", "));
            log.warn("Cannot mark details collected for program {}. Missing submissions for: {}", programId, missingIds);
            throw new IllegalArgumentException(
                    "Cannot move to next step. The following approved sewadars have not submitted forms: " + missingIds);
        }

        // All approved sewadars submitted forms, we can move to next node
        workflow.setDetailsCollected(true);
        workflow.setCurrentNode(5);
        workflow = workflowRepository.save(workflow);
        log.info("Details collected for program {}", programId);

        // Send notifications immediately for node 5
        sendNotificationsForNode(program, workflow);

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
                
                sendNotificationsForNode(program, workflow);
            } catch (Exception e) {
                log.error("Error sending notification for workflow {}", workflow.getId(), e);
            }
        }
    }

    /**
     * Check if notification is enabled for a program at a specific node.
     * Returns: program-level setting only.
     */
    private Boolean isNotificationEnabled(Program program, Integer nodeNumber) {
        com.rssb.application.entity.ProgramNotificationPreference programPref =
                programNotificationPreferenceRepository
                        .findByProgramAndNodeNumber(program, nodeNumber)
                        .orElse(null);

        return programPref != null && Boolean.TRUE.equals(programPref.getEnabled());
    }

    /**
     * Send notifications for a specific workflow node for a single program.
     * This is used both by the daily scheduler and immediate node transitions.
     */
    private void sendNotificationsForNode(Program program, ProgramWorkflow workflow) {
        if (program == null || workflow == null || workflow.getCurrentNode() == null) {
            return;
        }

        Integer currentNode = workflow.getCurrentNode();

        // Check program-level notification toggle
        Boolean notificationEnabled = isNotificationEnabled(program, currentNode);
        if (notificationEnabled == null || !notificationEnabled) {
            return;
        }

        // Get global notification message template (still used for message text)
        NotificationPreference globalPreference = notificationPreferenceRepository
                .findByNodeNumber(currentNode)
                .orElse(null);

        if (globalPreference == null || globalPreference.getNotificationMessage() == null) {
            return;
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
            workflow = workflowRepository.save(workflow);
            log.info("Workflow auto-advanced to node 2 for program {}", program.getId());

            // Send notifications immediately for node 2
            sendNotificationsForNode(program, workflow);
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
                workflow = workflowRepository.save(workflow);
                log.info("Workflow auto-advanced to node 3 for program {} (applications full)", program.getId());

                // Send notifications immediately for node 3
                sendNotificationsForNode(program, workflow);
            }
        }
    }

    /**
     * Get approved sewadars for a program who have NOT yet submitted forms.
     */
    @Transactional(readOnly = true)
    public List<com.rssb.application.dto.SewadarResponse> getMissingFormSubmitters(Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        List<ProgramApplication> approvedApps = applicationRepository
                .findByProgramIdAndStatus(programId, "APPROVED");

        List<Sewadar> missing = new java.util.ArrayList<>();
        for (ProgramApplication app : approvedApps) {
            boolean hasSubmission = formSubmissionRepository
                    .findByProgramIdAndSewadarZonalId(programId, app.getSewadar().getZonalId())
                    .isPresent();
            if (!hasSubmission) {
                missing.add(app.getSewadar());
            }
        }

        return missing.stream()
                .map(sewadar -> com.rssb.application.dto.SewadarResponse.builder()
                        .zonalId(sewadar.getZonalId())
                        .firstName(sewadar.getFirstName())
                        .lastName(sewadar.getLastName())
                        .mobile(sewadar.getMobile())
                        .location(sewadar.getLocation())
                        .role(sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR")
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Notify all approved sewadars who have not yet submitted forms.
     */
    public void notifyMissingFormSubmitters(Long programId) {
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        List<com.rssb.application.dto.SewadarResponse> missing = getMissingFormSubmitters(programId);

        if (missing.isEmpty()) {
            log.info("No missing form submitters for program {}", programId);
            return;
        }

        // Use the notification template for node 4 (Collect Details) if available
        NotificationPreference preference = notificationPreferenceRepository
                .findByNodeNumber(4)
                .orElse(null);

        String baseMessage;
        if (preference != null && preference.getNotificationMessage() != null) {
            baseMessage = preference.getNotificationMessage()
                    .replace("{programTitle}", program.getTitle());
        } else {
            baseMessage = "Please submit your travel details form for the program '" + program.getTitle() + "'.";
        }

        for (com.rssb.application.dto.SewadarResponse sewadar : missing) {
            if (sewadar.getMobile() != null && !sewadar.getMobile().isEmpty()) {
                whatsAppService.sendMessage(sewadar.getMobile(), baseMessage);
                log.info("Sent missing form reminder to sewadar {} for program {}", sewadar.getZonalId(), programId);
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

