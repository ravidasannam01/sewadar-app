package com.rssb.application.service;

import com.rssb.application.dto.PrioritizedApplicationResponse;
import com.rssb.application.dto.ProgramApplicationRequest;
import com.rssb.application.dto.ProgramApplicationResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramApplication;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.AttendanceRepository;
import com.rssb.application.repository.ProgramApplicationRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    @Override
    public ProgramApplicationResponse applyToProgram(ProgramApplicationRequest request) {
        log.info("Sewadar {} applying to program {}", request.getSewadarId(), request.getProgramId());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        // Check program status - only active programs can receive applications
        if (!"active".equalsIgnoreCase(program.getStatus())) {
            throw new IllegalArgumentException("Applications can only be submitted to active programs. Current program status: " + program.getStatus());
        }

        Sewadar sewadar = sewadarRepository.findByZonalId(request.getSewadarId())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", request.getSewadarId()));

        // Check if already applied
        Optional<ProgramApplication> existingOpt = applicationRepository.findByProgramIdAndSewadarZonalId(
                request.getProgramId(), request.getSewadarId());
        
        if (existingOpt.isPresent()) {
            ProgramApplication existing = existingOpt.get();
            
            // If status is PENDING, reject (already has active application)
            if ("PENDING".equals(existing.getStatus()) || "APPROVED".equals(existing.getStatus())) {
                throw new IllegalArgumentException("Sewadar has already applied to this program with status: " + existing.getStatus());
            }
            
            // If DROPPED, update existing application to PENDING (reapply)
            // Keep drop history fields (drop_requested_at, drop_approved_at, drop_approved_by) as proof of previous drop
            if ("DROPPED".equals(existing.getStatus())) {
                existing.setStatus("PENDING");
                existing.setNotes(request.getNotes());
                existing.setAppliedAt(java.time.LocalDateTime.now());
                // DO NOT clear drop history fields - they serve as audit trail
                // drop_requested_at, drop_approved_at, drop_approved_by remain unchanged
                
                ProgramApplication saved = applicationRepository.save(existing);
                log.info("Application {} updated from DROPPED to PENDING (reapply), drop history preserved", saved.getId());
                return mapToResponse(saved);
            }
        }

        // Create new application if no existing application found
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
    public List<ProgramApplicationResponse> getApplicationsBySewadar(Long sewadarZonalId) {
        log.info("Fetching applications for sewadar: {}", sewadarZonalId);
        return applicationRepository.findBySewadarZonalId(sewadarZonalId).stream()
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
        
        // If sewadar drops consent, log it
        if ("REJECTED".equals(status) || "DROPPED".equals(status)) {
            log.info("Sewadar {} dropped consent for program {}", 
                    application.getSewadar().getZonalId(), 
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
        if (!application.getSewadar().getZonalId().equals(sewadarId)) {
            throw new IllegalArgumentException("Sewadar can only drop their own applications");
        }
        
        // Check if already dropped or drop requested
        if ("DROPPED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Application is already dropped");
        }
        if ("DROP_REQUESTED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Drop request already pending");
        }
        
        // Update existing application row to DROP_REQUESTED (reuse same row)
        application.setStatus("DROP_REQUESTED");
        application.setDropRequestedAt(java.time.LocalDateTime.now());
        
        ProgramApplication saved = applicationRepository.save(application);
        log.info("Drop request created for application {}", applicationId);
        
        return mapToResponse(saved);
    }

    @Override
    public ProgramApplicationResponse approveDropRequest(Long applicationId, Long inchargeId, Boolean allowReapply) {
        log.info("Incharge {} approving drop request for application {}, allowReapply: {}", 
                inchargeId, applicationId, allowReapply);
        
        ProgramApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramApplication", "id", applicationId));
        
        // Verify incharge created the program
        if (!application.getProgram().getCreatedBy().getZonalId().equals(inchargeId)) {
            throw new IllegalArgumentException("Only program creator can approve drop requests");
        }
        
        // Verify status is DROP_REQUESTED
        if (!"DROP_REQUESTED".equals(application.getStatus())) {
            throw new IllegalArgumentException("Application is not in DROP_REQUESTED status");
        }
        
        // Update application status - always allow reapply
        application.setStatus("DROPPED");
        application.setDropApprovedAt(java.time.LocalDateTime.now());
        application.setDropApprovedBy(inchargeId);
        
        ProgramApplication saved = applicationRepository.save(application);
        
        log.info("Drop request approved for application {}", applicationId);
        
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
                    Long totalAttendance = attendanceRepository.countAttendedProgramsBySewadarId(sewadar.getZonalId());
                    Long beasAttendance = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "BEAS");
                    Long nonBeasAttendance = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "NON_BEAS");
                    
                    Integer totalDays = attendanceRepository.sumDaysAttendedBySewadarId(sewadar.getZonalId());
                    Integer beasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "BEAS");
                    Integer nonBeasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "NON_BEAS");
                    
                    // Calculate priority score (weighted)
                    // Higher attendance = higher score
                    Long priorityScore = (totalAttendance * 10L) + (totalDays != null ? totalDays : 0);
                    
                    SewadarResponse sewadarResponse = SewadarResponse.builder()
                            .zonalId(sewadar.getZonalId())
                            .firstName(sewadar.getFirstName())
                            .lastName(sewadar.getLastName())
                            .mobile(sewadar.getMobile())
                            .location(sewadar.getLocation())
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
                .zonalId(application.getSewadar().getZonalId())
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
                .dropRequestedAt(application.getDropRequestedAt())
                .dropApprovedAt(application.getDropApprovedAt())
                .dropApprovedBy(application.getDropApprovedBy())
                .build();
    }
}

