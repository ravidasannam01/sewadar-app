package com.rssb.application.service;

import com.rssb.application.dto.ProgramWorkflowResponse;
import com.rssb.application.entity.*;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final EmailService emailService;
    
    // Notification configuration for all workflow steps
    @Value("${notification.step1.recipients:INCHARGES}")
    private String step1Recipients;
    
    @Value("${notification.step1.whatsapp.enabled:false}")
    private boolean step1WhatsAppEnabled;
    
    @Value("${notification.step1.email.enabled:true}")
    private boolean step1EmailEnabled;
    
    @Value("${notification.step2.recipients:INCHARGES}")
    private String step2Recipients;
    
    @Value("${notification.step2.whatsapp.enabled:false}")
    private boolean step2WhatsAppEnabled;
    
    @Value("${notification.step2.email.enabled:true}")
    private boolean step2EmailEnabled;
    
    @Value("${notification.step3.recipients:INCHARGES}")
    private String step3Recipients;
    
    @Value("${notification.step3.whatsapp.enabled:false}")
    private boolean step3WhatsAppEnabled;
    
    @Value("${notification.step3.email.enabled:true}")
    private boolean step3EmailEnabled;
    
    @Value("${notification.step4.recipients:INCHARGES}")
    private String step4Recipients;
    
    @Value("${notification.step4.whatsapp.enabled:false}")
    private boolean step4WhatsAppEnabled;
    
    @Value("${notification.step4.email.enabled:true}")
    private boolean step4EmailEnabled;
    
    @Value("${notification.step5.recipients:INCHARGES}")
    private String step5Recipients;
    
    @Value("${notification.step5.whatsapp.enabled:false}")
    private boolean step5WhatsAppEnabled;
    
    @Value("${notification.step5.email.enabled:true}")
    private boolean step5EmailEnabled;
    
    @Value("${notification.step6.recipients:INCHARGES}")
    private String step6Recipients;
    
    @Value("${notification.step6.whatsapp.enabled:false}")
    private boolean step6WhatsAppEnabled;
    
    @Value("${notification.step6.email.enabled:true}")
    private boolean step6EmailEnabled;
    
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
     * Creates workflow starting at node 1 and initializes all notification preferences as enabled by default.
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
                workflow = workflowRepository.save(workflow);
                
                // Initialize all 6 notification preferences as enabled by default
                for (int nodeNumber = 1; nodeNumber <= 6; nodeNumber++) {
                    // Check if preference already exists (shouldn't, but safety check)
                    com.rssb.application.entity.ProgramNotificationPreference existing = 
                        programNotificationPreferenceRepository
                            .findByProgramAndNodeNumber(program, nodeNumber)
                            .orElse(null);
                    
                    if (existing == null) {
                        com.rssb.application.entity.ProgramNotificationPreference preference = 
                            com.rssb.application.entity.ProgramNotificationPreference.builder()
                                .program(program)
                                .nodeNumber(nodeNumber)
                                .enabled(true) // All nodes enabled by default
                                .build();
                        programNotificationPreferenceRepository.save(preference);
                        log.debug("Initialized notification preference for program {} node {} as enabled", 
                                program.getId(), nodeNumber);
                    }
                }
                
                log.info("Initialized workflow for program {} with all notification preferences enabled", program.getId());
                return workflow;
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
     * Called by scheduler or manual trigger.
     */
    public void sendDailyNotifications() {
        log.info("[NOTIFICATION TRIGGER] Starting notification process for all programs");
        List<ProgramWorkflow> workflows = workflowRepository.findAll();
        log.info("[NOTIFICATION TRIGGER] Found {} workflows to process", workflows.size());
        
        int processedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;
        
        for (ProgramWorkflow workflow : workflows) {
            try {
                // Use program ID from workflow's program reference, but fetch fresh to avoid lazy loading
                Long programId = workflow.getProgram() != null ? workflow.getProgram().getId() : null;
                if (programId == null) {
                    log.warn("[NOTIFICATION TRIGGER] Skipping workflow {}: No program ID", workflow.getId());
                    skippedCount++;
                    continue;
                }
                Program program = programRepository.findById(programId)
                    .orElse(null);
                
                if (program == null) {
                    log.warn("[NOTIFICATION TRIGGER] Skipping workflow {}: Program {} not found", workflow.getId(), programId);
                    skippedCount++;
                    continue;
                }
                
                log.info("[NOTIFICATION TRIGGER] Processing workflow {} for program {} (ID: {})", 
                        workflow.getId(), program.getTitle(), programId);
                sendNotificationsForNode(program, workflow);
                processedCount++;
            } catch (Exception e) {
                log.error("[NOTIFICATION TRIGGER] Error sending notification for workflow {}: {}", 
                        workflow.getId(), e.getMessage(), e);
                errorCount++;
            }
        }
        
        log.info("[NOTIFICATION TRIGGER] Completed: Processed={}, Skipped={}, Errors={}", 
                processedCount, skippedCount, errorCount);
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
     * Supports configurable recipients (INCHARGES, ALL, APPROVED) and both WhatsApp/Email.
     */
    private void sendNotificationsForNode(Program program, ProgramWorkflow workflow) {
        if (program == null || workflow == null || workflow.getCurrentNode() == null) {
            log.debug("Skipping notification: program={}, workflow={}, currentNode={}", 
                    program != null ? program.getId() : "null", 
                    workflow != null ? workflow.getId() : "null",
                    workflow != null ? workflow.getCurrentNode() : "null");
            return;
        }

        Integer currentNode = workflow.getCurrentNode();
        log.info("[NOTIFICATION CHECK] Program: {} (ID: {}) | Current Node: {}", 
                program.getTitle(), program.getId(), currentNode);

        // Check program-level notification toggle
        Boolean notificationEnabled = isNotificationEnabled(program, currentNode);
        if (notificationEnabled == null || !notificationEnabled) {
            log.info("[NOTIFICATION SKIPPED] Program: {} (ID: {}) | Node: {} | Reason: Notification toggle is OFF (enabled={})", 
                    program.getTitle(), program.getId(), currentNode, notificationEnabled);
            return;
        }

        log.info("[NOTIFICATION ENABLED] Program: {} (ID: {}) | Node: {} | Notification toggle is ON", 
                program.getTitle(), program.getId(), currentNode);

        // Ensure global notification preferences are initialized (if empty)
        if (notificationPreferenceRepository.count() == 0) {
            log.info("[NOTIFICATION INIT] Initializing global notification preferences");
            try {
                // Initialize defaults in a new transaction to avoid conflicts
                String[] nodeNames = {
                    "Make Program Active",
                    "Post Application Message",
                    "Release Form",
                    "Collect Details",
                    "Post Mail to Area Secretary",
                    "Post General Instructions"
                };
                String[] messages = {
                    "Please make the program '{programTitle}' active.", // Node 1: Make Program Active
                    "Please post a message in the central WhatsApp group asking sewadars to apply for '{programTitle}' before a specific date.", // Node 2: Post Application Message
                    "Applications are full. Please post a message in WhatsApp group with form details for '{programTitle}'.", // Node 3: Release Form
                    "Please collect travel details from all approved sewadars for the program '{programTitle}'.", // Node 4: Collect Details
                    "Please post a mail to the area secretary regarding '{programTitle}' at their area.", // Node 5: Post Mail to Area Secretary
                    "Please post general instructions regarding '{programTitle}' in the WhatsApp group." // Node 6: Post General Instructions
                };
                for (int i = 0; i < nodeNames.length; i++) {
                    NotificationPreference preference = NotificationPreference.builder()
                        .nodeNumber(i + 1)
                        .nodeName(nodeNames[i])
                        .notificationMessage(messages[i])
                        .enabled(true)
                        .build();
                    notificationPreferenceRepository.save(preference);
                }
                log.info("[NOTIFICATION INIT] Successfully initialized {} global notification preferences", nodeNames.length);
            } catch (Exception e) {
                log.error("[NOTIFICATION INIT] Error initializing global notification preferences: {}", e.getMessage(), e);
            }
        }
        
        // Get message template: check program-level first, then fallback to global
        String messageTemplate = null;
        
        // Check for program-level custom message
        ProgramNotificationPreference programPreference = programNotificationPreferenceRepository
                .findByProgramAndNodeNumber(program, currentNode)
                .orElse(null);
        
        if (programPreference != null && programPreference.getMessage() != null 
                && !programPreference.getMessage().trim().isEmpty()) {
            messageTemplate = programPreference.getMessage();
            log.debug("[NOTIFICATION MESSAGE] Program: {} (ID: {}) | Node: {} | Using program-level custom message", 
                    program.getTitle(), program.getId(), currentNode);
        } else {
            // Fallback to global message
            NotificationPreference globalPreference = notificationPreferenceRepository
                    .findByNodeNumber(currentNode)
                    .orElse(null);
            
            if (globalPreference != null && globalPreference.getNotificationMessage() != null) {
                messageTemplate = globalPreference.getNotificationMessage();
                log.debug("[NOTIFICATION MESSAGE] Program: {} (ID: {}) | Node: {} | Using global default message", 
                        program.getTitle(), program.getId(), currentNode);
            }
        }

        if (messageTemplate == null || messageTemplate.trim().isEmpty()) {
            log.warn("[NOTIFICATION SKIPPED] Program: {} (ID: {}) | Node: {} | Reason: No notification message template found for node {}", 
                    program.getTitle(), program.getId(), currentNode, currentNode);
            return;
        }

        // Determine recipients and notification methods based on node and configuration
        List<Sewadar> recipients = getRecipientsForNode(program, currentNode);
        boolean useWhatsApp = shouldUseWhatsApp(currentNode);
        boolean useEmail = shouldUseEmail(currentNode);

        log.info("[NOTIFICATION CONFIG] Program: {} (ID: {}) | Node: {} | Recipients: {} | WhatsApp: {} | Email: {}", 
                program.getTitle(), program.getId(), currentNode, recipients.size(), useWhatsApp, useEmail);

        if (recipients.isEmpty()) {
            log.warn("[NOTIFICATION SKIPPED] Program: {} (ID: {}) | Node: {} | Reason: No recipients found", 
                    program.getTitle(), program.getId(), currentNode);
            return;
        }

        // Prepare message based on recipient type
        String message = prepareMessageForRecipients(messageTemplate, 
                program, currentNode, recipients);

        // Log the message being sent (truncate if too long for readability)
        String messagePreview = message.length() > 150 ? message.substring(0, 150) + "..." : message;
        log.info("[NOTIFICATION MESSAGE] Program: {} (ID: {}) | Node: {} | Message: {}", 
                program.getTitle(), program.getId(), currentNode, messagePreview);

        // Send notifications
        int emailSentCount = 0;
        int whatsappSentCount = 0;
        for (Sewadar recipient : recipients) {
            if (useWhatsApp && recipient.getMobile() != null && !recipient.getMobile().isEmpty()) {
                boolean whatsappSent = whatsAppService.sendMessage(recipient.getMobile(), message);
                if (whatsappSent) {
                    whatsappSentCount++;
                }
                log.info("[WHATSAPP ATTEMPT] Program: {} | Node: {} | Recipient: {} ({}) | Mobile: {} | Status: {} | Message: {}", 
                        program.getTitle(), currentNode, recipient.getZonalId(), 
                        recipient.getFirstName() + " " + recipient.getLastName(), 
                        recipient.getMobile(), whatsappSent ? "SENT" : "FAILED", messagePreview);
            } else if (useWhatsApp && (recipient.getMobile() == null || recipient.getMobile().isEmpty())) {
                log.warn("[WHATSAPP SKIPPED] Program: {} | Node: {} | Recipient: {} | Reason: No mobile number", 
                        program.getTitle(), currentNode, recipient.getZonalId());
            }
            
            if (useEmail && recipient.getEmailId() != null && !recipient.getEmailId().isEmpty()) {
                String subject = "Program Notification: " + program.getTitle();
                boolean emailSent = emailService.sendEmail(recipient.getEmailId(), subject, message);
                if (emailSent) {
                    emailSentCount++;
                }
                log.info("[EMAIL ATTEMPT] Program: {} | Node: {} | Recipient: {} ({}) | Email: {} | Status: {} | Message: {}", 
                        program.getTitle(), currentNode, recipient.getZonalId(), 
                        recipient.getFirstName() + " " + recipient.getLastName(), 
                        recipient.getEmailId(), emailSent ? "SENT" : "FAILED", messagePreview);
            } else if (useEmail && (recipient.getEmailId() == null || recipient.getEmailId().isEmpty())) {
                log.warn("[EMAIL SKIPPED] Program: {} | Node: {} | Recipient: {} | Reason: No email ID", 
                        program.getTitle(), currentNode, recipient.getZonalId());
            }
        }

        log.info("[NOTIFICATION SUMMARY] Program: {} (ID: {}) | Node: {} | Total Recipients: {} | Emails Sent: {} | WhatsApp Sent: {}", 
                program.getTitle(), program.getId(), currentNode, recipients.size(), emailSentCount, whatsappSentCount);
    }

    /**
     * Get recipients for a specific workflow node based on configuration.
     */
    private List<Sewadar> getRecipientsForNode(Program program, Integer nodeNumber) {
        String recipientType;
        
        // Get recipient type from configuration for each step
        switch (nodeNumber) {
            case 1:
                recipientType = step1Recipients.toUpperCase();
                break;
            case 2:
                recipientType = step2Recipients.toUpperCase();
                break;
            case 3:
                recipientType = step3Recipients.toUpperCase();
                break;
            case 4:
                recipientType = step4Recipients.toUpperCase();
                break;
            case 5:
                recipientType = step5Recipients.toUpperCase();
                break;
            case 6:
                recipientType = step6Recipients.toUpperCase();
                break;
            default:
                recipientType = "INCHARGES";
                break;
        }

        switch (recipientType) {
            case "ALL":
                // Send to all sewadars (for step 2: direct message to apply)
                return sewadarRepository.findAll();
                
            case "APPROVED":
                // Send only to approved applicants (for step 3: form release)
                List<ProgramApplication> approvedApps = applicationRepository
                        .findByProgramIdAndStatus(program.getId(), "APPROVED");
                return approvedApps.stream()
                        .map(ProgramApplication::getSewadar)
                        .collect(Collectors.toList());
                
            case "INCHARGES":
            default:
                // Send to both ADMIN and INCHARGE (alert/reminder to post in community)
                List<Sewadar> admins = sewadarRepository.findByRole(Role.ADMIN);
                List<Sewadar> incharges = sewadarRepository.findByRole(Role.INCHARGE);
                List<Sewadar> allRecipients = new ArrayList<>();
                allRecipients.addAll(admins);
                allRecipients.addAll(incharges);
                return allRecipients;
        }
    }

    /**
     * Check if WhatsApp should be used for this node.
     */
    private boolean shouldUseWhatsApp(Integer nodeNumber) {
        switch (nodeNumber) {
            case 1:
                return step1WhatsAppEnabled;
            case 2:
                return step2WhatsAppEnabled;
            case 3:
                return step3WhatsAppEnabled;
            case 4:
                return step4WhatsAppEnabled;
            case 5:
                return step5WhatsAppEnabled;
            case 6:
                return step6WhatsAppEnabled;
            default:
                return false;
        }
    }

    /**
     * Check if Email should be used for this node.
     */
    private boolean shouldUseEmail(Integer nodeNumber) {
        switch (nodeNumber) {
            case 1:
                return step1EmailEnabled;
            case 2:
                return step2EmailEnabled;
            case 3:
                return step3EmailEnabled;
            case 4:
                return step4EmailEnabled;
            case 5:
                return step5EmailEnabled;
            case 6:
                return step6EmailEnabled;
            default:
                return false;
        }
    }

    /**
     * Prepare message text based on recipient type.
     * Different messages for INCHARGES (alert) vs ALL/APPROVED (direct message).
     */
    private String prepareMessageForRecipients(String baseMessage, Program program, 
            Integer nodeNumber, List<Sewadar> recipients) {
        String message = baseMessage.replace("{programTitle}", program.getTitle());
        
        // Determine recipient type
        String recipientType;
        if (nodeNumber == 2) {
            recipientType = step2Recipients.toUpperCase();
        } else if (nodeNumber == 3) {
            recipientType = step3Recipients.toUpperCase();
        } else {
            recipientType = "INCHARGES";
        }

        // Customize message based on recipient type and node
        if (nodeNumber == 2) {
            if ("INCHARGES".equals(recipientType)) {
                // Alert/reminder for incharges to post in community group
                message = "Reminder: Please post an application message in the community group for program '" + 
                        program.getTitle() + "'. " + message;
            } else if ("ALL".equals(recipientType)) {
                // Direct message to all sewadars to apply
                message = "New program available: '" + program.getTitle() + "'. " + 
                        "Please apply for this program through the application system. " + message;
            }
        } else if (nodeNumber == 3) {
            if ("INCHARGES".equals(recipientType)) {
                // Alert for incharges
                message = "Reminder: Form has been released for program '" + program.getTitle() + "'. " + message;
            } else if ("APPROVED".equals(recipientType)) {
                // Direct message to approved applicants
                message = "Form is now available for program '" + program.getTitle() + 
                        "'. Please submit your travel details form. " + message;
            }
        }

        return message;
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
                        .emailId(sewadar.getEmailId()) // Added emailId field
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

        // Use a specific message for missing form reminders (not node 4's message)
        String baseMessage = "Please submit your travel details form for the program '" + program.getTitle() + "'. The form submission deadline is approaching.";

        // Send via both WhatsApp and Email if configured
        boolean useWhatsApp = step3WhatsAppEnabled; // Use step3 config for form reminders
        boolean useEmail = step3EmailEnabled;
        
        // Log the message being sent (truncate if too long for readability)
        String messagePreview = baseMessage.length() > 150 ? baseMessage.substring(0, 150) + "..." : baseMessage;
        log.info("[MISSING FORMS NOTIFICATION] Program: {} (ID: {}) | Message: {} | Recipients: {} | WhatsApp: {} | Email: {}", 
                program.getTitle(), program.getId(), messagePreview, missing.size(), useWhatsApp, useEmail);
        
        if (missing.isEmpty()) {
            log.warn("[MISSING FORMS] No missing form submitters found - skipping notification");
            return;
        }
        
        log.info("[MISSING FORMS] Starting to send notifications to {} recipients", missing.size());
        
        int emailSentCount = 0;
        int emailFailedCount = 0;
        int whatsappSentCount = 0;
        int whatsappFailedCount = 0;
        
        for (com.rssb.application.dto.SewadarResponse sewadar : missing) {
            log.debug("[MISSING FORMS] Processing recipient: {} ({})", sewadar.getZonalId(), sewadar.getEmailId());
            
            if (useWhatsApp && sewadar.getMobile() != null && !sewadar.getMobile().isEmpty()) {
                try {
                    boolean whatsappSent = whatsAppService.sendMessage(sewadar.getMobile(), baseMessage);
                    if (whatsappSent) {
                        whatsappSentCount++;
                    } else {
                        whatsappFailedCount++;
                    }
                    log.info("[MISSING FORMS WHATSAPP] Program: {} | Recipient: {} ({}) | Mobile: {} | Status: {} | Message: {}", 
                            program.getTitle(), sewadar.getZonalId(), 
                            sewadar.getFirstName() + " " + sewadar.getLastName(), 
                            sewadar.getMobile(), whatsappSent ? "SENT" : "FAILED", messagePreview);
                } catch (Exception e) {
                    whatsappFailedCount++;
                    log.error("[MISSING FORMS WHATSAPP] Failed to send to {}: {}", sewadar.getZonalId(), e.getMessage(), e);
                }
            } else if (useWhatsApp) {
                log.warn("[MISSING FORMS WHATSAPP] Skipped {} - No mobile number", sewadar.getZonalId());
            }
            
            if (useEmail && sewadar.getEmailId() != null && !sewadar.getEmailId().isEmpty()) {
                try {
                    String subject = "Reminder: Submit Form for " + program.getTitle();
                    boolean emailSent = emailService.sendEmail(sewadar.getEmailId(), subject, baseMessage);
                    if (emailSent) {
                        emailSentCount++;
                    } else {
                        emailFailedCount++;
                    }
                    log.info("[MISSING FORMS EMAIL] Program: {} | Recipient: {} ({}) | Email: {} | Status: {} | Message: {}", 
                            program.getTitle(), sewadar.getZonalId(), 
                            sewadar.getFirstName() + " " + sewadar.getLastName(), 
                            sewadar.getEmailId(), emailSent ? "SENT" : "FAILED", messagePreview);
                } catch (Exception e) {
                    emailFailedCount++;
                    log.error("[MISSING FORMS EMAIL] Failed to send to {}: {}", sewadar.getEmailId(), e.getMessage(), e);
                }
            } else if (useEmail) {
                log.warn("[MISSING FORMS EMAIL] Skipped {} - No email ID", sewadar.getZonalId());
            }
        }
        
        log.info("[MISSING FORMS SUMMARY] Program: {} (ID: {}) | Emails: {} sent, {} failed | WhatsApp: {} sent, {} failed", 
                program.getTitle(), program.getId(), emailSentCount, emailFailedCount, whatsappSentCount, whatsappFailedCount);
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

