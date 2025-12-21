package com.rssb.application.service;

import com.rssb.application.dto.PrioritizedApplicationResponse;
import com.rssb.application.dto.ProgramApplicationRequest;
import com.rssb.application.dto.ProgramApplicationResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramApplication;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.entity.Notification;
import com.rssb.application.repository.AttendanceRepository;
import com.rssb.application.repository.NotificationRepository;
import com.rssb.application.repository.ProgramApplicationRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.ProgramSelectionRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgramApplicationServiceImpl implements ProgramApplicationService {

    private final ProgramApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProgramSelectionRepository selectionRepository;
    private final NotificationRepository notificationRepository;

    @Override
    public ProgramApplicationResponse applyToProgram(ProgramApplicationRequest request) {
        log.info("Sewadar {} applying to program {}", request.getSewadarId(), request.getProgramId());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        Sewadar sewadar = sewadarRepository.findById(request.getSewadarId())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getSewadarId()));

        // Check if already applied
        applicationRepository.findByProgramIdAndSewadarId(request.getProgramId(), request.getSewadarId())
                .ifPresent(existing -> {
                    // Check if previous application was dropped and reapply is not allowed
                    if ("DROPPED".equals(existing.getStatus()) && Boolean.FALSE.equals(existing.getReapplyAllowed())) {
                        throw new IllegalArgumentException("You are not allowed to reapply for this program");
                    }
                    // If not dropped or reapply allowed, check if there's an active application
                    if (!"DROPPED".equals(existing.getStatus())) {
                        throw new IllegalArgumentException("Sewadar has already applied to this program");
                    }
                    // If dropped and reapply allowed, delete old application to create new one
                    applicationRepository.delete(existing);
                    log.info("Deleted old dropped application to allow reapply");
                });

        ProgramApplication application = ProgramApplication.builder()
                .program(program)
                .sewadar(sewadar)
                .notes(request.getNotes())
                .status("PENDING")
                .build();

        ProgramApplication saved = applicationRepository.save(application);
        log.info("Application created with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramApplicationResponse> getApplicationsByProgram(Long programId) {
        log.info("Fetching applications for program: {}", programId);
        // Filter out DROPPED applications for incharge view
        return applicationRepository.findByProgramIdAndStatusNot(programId, "DROPPED").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramApplicationResponse> getApplicationsBySewadar(Long sewadarId) {
        log.info("Fetching applications for sewadar: {}", sewadarId);
        return applicationRepository.findBySewadarId(sewadarId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramApplicationResponse updateApplicationStatus(Long id, String status) {
        log.info("Updating application {} status to {}", id, status);
        ProgramApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramApplication", "id", id));

        application.setStatus(status);
        ProgramApplication updated = applicationRepository.save(application);
        
        // If sewadar drops consent, notify incharge
        if ("REJECTED".equals(status) || "DROPPED".equals(status)) {
            // TODO: Notify incharge via WhatsApp service
            log.info("Sewadar {} dropped consent for program {}", 
                    application.getSewadar().getId(), 
                    application.getProgram().getId());
        }
        
        return mapToResponse(updated);
    }

    @Override
    public void deleteApplication(Long id) {
        log.info("Deleting application: {}", id);
        ProgramApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramApplication", "id", id));
        applicationRepository.delete(application);
    }

    @Override
    public ProgramApplicationResponse requestDrop(Long applicationId, Long sewadarId) {
        log.info("Sewadar {} requesting to drop application {}", sewadarId, applicationId);
        
        ProgramApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramApplication", "id", applicationId));
        
        // Verify sewadar owns this application
        if (!application.getSewadar().getId().equals(sewadarId)) {
            throw new IllegalArgumentException("Sewadar can only drop their own applications");
        }
        
        // Check if already dropped or drop requested
        if ("DROPPED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Application is already dropped");
        }
        if ("DROP_REQUESTED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Drop request already pending");
        }
        
        // Set status to DROP_REQUESTED
        application.setStatus("DROP_REQUESTED");
        application.setDropRequestedAt(java.time.LocalDateTime.now());
        
        ProgramApplication saved = applicationRepository.save(application);
        log.info("Drop request created for application {}", applicationId);
        
        // TODO: Notify incharge via WhatsApp
        
        return mapToResponse(saved);
    }

    @Override
    public ProgramApplicationResponse approveDropRequest(Long applicationId, Long inchargeId, Boolean allowReapply) {
        log.info("Incharge {} approving drop request for application {}, allowReapply: {}", 
                inchargeId, applicationId, allowReapply);
        
        ProgramApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramApplication", "id", applicationId));
        
        // Verify incharge created the program
        if (!application.getProgram().getCreatedBy().getId().equals(inchargeId)) {
            throw new IllegalArgumentException("Only program creator can approve drop requests");
        }
        
        // Verify status is DROP_REQUESTED
        if (!"DROP_REQUESTED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Application is not in DROP_REQUESTED status");
        }
        
        // Update application status
        application.setStatus("DROPPED");
        application.setDropApprovedAt(java.time.LocalDateTime.now());
        application.setDropApprovedBy(inchargeId);
        application.setReapplyAllowed(allowReapply != null ? allowReapply : true);
        
        ProgramApplication saved = applicationRepository.save(application);
        
        // Also update selection status if exists
        selectionRepository.findByProgramIdAndSewadarId(
                application.getProgram().getId(), 
                application.getSewadar().getId())
                .ifPresent(selection -> {
                    selection.setStatus("DROPPED");
                    selectionRepository.save(selection);
                    log.info("Selection {} also marked as DROPPED", selection.getId());
                });
        
        // Create notification for incharge to refill the position
        Program program = application.getProgram();
        Sewadar incharge = program.getCreatedBy();
        Sewadar droppedSewadar = application.getSewadar();
        
        Notification notification = Notification.builder()
                .program(program)
                .droppedSewadar(droppedSewadar)
                .incharge(incharge)
                .notificationType("REFILL_REQUIRED")
                .message(String.format("Sewadar %s %s dropped from program '%s'. Please refill the position.",
                        droppedSewadar.getFirstName(), droppedSewadar.getLastName(), program.getTitle()))
                .resolved(false)
                .build();
        
        notificationRepository.save(notification);
        log.info("Notification created for incharge {} to refill position in program {}", 
                incharge.getId(), program.getId());
        
        log.info("Drop request approved for application {}", applicationId);
        
        // TODO: Notify sewadar via WhatsApp
        
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramApplicationResponse> getDropRequestsByProgram(Long programId) {
        log.info("Fetching drop requests for program: {}", programId);
        return applicationRepository.findByProgramIdAndStatus(programId, "DROP_REQUESTED").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrioritizedApplicationResponse> getPrioritizedApplications(
            Long programId, String sortBy, String order) {
        log.info("Fetching prioritized applications for program: {}, sortBy: {}, order: {}", 
                programId, sortBy, order);
        
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));
        
        // Filter out DROPPED applications from prioritized view
        List<ProgramApplication> applications = applicationRepository.findByProgramIdAndStatusNot(programId, "DROPPED");
        
        List<PrioritizedApplicationResponse> prioritized = applications.stream()
                .map(app -> {
                    Sewadar sewadar = app.getSewadar();
                    
                    // Calculate attendance metrics
                    Long totalAttendance = attendanceRepository.countAttendedProgramsBySewadarId(sewadar.getId());
                    Long beasAttendance = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getId(), "BEAS");
                    Long nonBeasAttendance = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getId(), "NON_BEAS");
                    
                    Integer totalDays = attendanceRepository.sumDaysAttendedBySewadarId(sewadar.getId());
                    Integer beasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getId(), "BEAS");
                    Integer nonBeasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getId(), "NON_BEAS");
                    
                    // Calculate priority score (weighted)
                    // Higher attendance = higher score
                    Long priorityScore = (totalAttendance * 10L) + (totalDays != null ? totalDays : 0);
                    
                    SewadarResponse sewadarResponse = SewadarResponse.builder()
                            .id(sewadar.getId())
                            .firstName(sewadar.getFirstName())
                            .lastName(sewadar.getLastName())
                            .mobile(sewadar.getMobile())
                            .dept(sewadar.getDept())
                            .profession(sewadar.getProfession())
                            .joiningDate(sewadar.getJoiningDate())
                            .role(sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR")
                            .build();
                    
                    return PrioritizedApplicationResponse.builder()
                            .id(app.getId())
                            .programId(programId)
                            .programTitle(program.getTitle())
                            .sewadar(sewadarResponse)
                            .appliedAt(app.getAppliedAt())
                            .status(app.getStatus())
                            .totalAttendanceCount(totalAttendance)
                            .beasAttendanceCount(beasAttendance)
                            .nonBeasAttendanceCount(nonBeasAttendance)
                            .totalDaysAttended(totalDays)
                            .beasDaysAttended(beasDays)
                            .nonBeasDaysAttended(nonBeasDays)
                            .profession(sewadar.getProfession())
                            .joiningDate(sewadar.getJoiningDate())
                            .priorityScore(priorityScore)
                            .build();
                })
                .collect(Collectors.toList());
        
        // Sort based on sortBy parameter
        Comparator<PrioritizedApplicationResponse> comparator = getComparator(sortBy);
        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }
        
        return prioritized.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }
    
    private Comparator<PrioritizedApplicationResponse> getComparator(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "priorityScore"; // Default sort
        }
        
        switch (sortBy.toLowerCase()) {
            case "attendance":
            case "totalattendance":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getTotalAttendanceCount() != null 
                                ? a.getTotalAttendanceCount() : 0L);
            case "beasattendance":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getBeasAttendanceCount() != null 
                                ? a.getBeasAttendanceCount() : 0L);
            case "nonbeasattendance":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getNonBeasAttendanceCount() != null 
                                ? a.getNonBeasAttendanceCount() : 0L);
            case "days":
            case "totaldays":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getTotalDaysAttended() != null 
                                ? a.getTotalDaysAttended() : 0);
            case "beasdays":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getBeasDaysAttended() != null 
                                ? a.getBeasDaysAttended() : 0);
            case "nonbeasdays":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getNonBeasDaysAttended() != null 
                                ? a.getNonBeasDaysAttended() : 0);
            case "profession":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getProfession() != null 
                                ? a.getProfession() : "");
            case "joiningdate":
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getJoiningDate() != null 
                                ? a.getJoiningDate() : java.time.LocalDate.MIN);
            case "priorityscore":
            default:
                return Comparator.comparing(
                        (PrioritizedApplicationResponse a) -> a.getPriorityScore() != null 
                                ? a.getPriorityScore() : 0L);
        }
    }

    private ProgramApplicationResponse mapToResponse(ProgramApplication application) {
        SewadarResponse sewadar = SewadarResponse.builder()
                .id(application.getSewadar().getId())
                .firstName(application.getSewadar().getFirstName())
                .lastName(application.getSewadar().getLastName())
                .mobile(application.getSewadar().getMobile())
                .build();

        return ProgramApplicationResponse.builder()
                .id(application.getId())
                .programId(application.getProgram().getId())
                .programTitle(application.getProgram().getTitle())
                .sewadar(sewadar)
                .appliedAt(application.getAppliedAt())
                .status(application.getStatus())
                .notes(application.getNotes())
                .reapplyAllowed(application.getReapplyAllowed())
                .dropRequestedAt(application.getDropRequestedAt())
                .dropApprovedAt(application.getDropApprovedAt())
                .build();
    }
}

