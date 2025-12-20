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

        Sewadar sewadar = sewadarRepository.findById(request.getSewadarId())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getSewadarId()));

        // Check if already applied
        applicationRepository.findByProgramIdAndSewadarId(request.getProgramId(), request.getSewadarId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Sewadar has already applied to this program");
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
        return applicationRepository.findByProgramId(programId).stream()
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
    @Transactional(readOnly = true)
    public List<PrioritizedApplicationResponse> getPrioritizedApplications(
            Long programId, String sortBy, String order) {
        log.info("Fetching prioritized applications for program: {}, sortBy: {}, order: {}", 
                programId, sortBy, order);
        
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));
        
        List<ProgramApplication> applications = applicationRepository.findByProgramId(programId);
        
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
                .build();
    }
}

