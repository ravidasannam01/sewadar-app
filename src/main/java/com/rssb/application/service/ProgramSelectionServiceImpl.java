package com.rssb.application.service;

import com.rssb.application.dto.ProgramSelectionRequest;
import com.rssb.application.dto.ProgramSelectionResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramSelection;
import com.rssb.application.entity.Role;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.AttendanceRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.ProgramSelectionRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProgramSelectionServiceImpl implements ProgramSelectionService {

    private final ProgramSelectionRepository selectionRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;
    private final AttendanceRepository attendanceRepository;
    private final com.rssb.application.repository.NotificationRepository notificationRepository;
    private final com.rssb.application.service.WhatsAppService whatsAppService;

    @Override
    public List<ProgramSelectionResponse> selectSewadars(ProgramSelectionRequest request) {
        log.info("Incharge {} selecting sewadars for program {}", request.getSelectedById(), request.getProgramId());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        Sewadar incharge = sewadarRepository.findById(request.getSelectedById())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getSelectedById()));

        if (incharge.getRole() != Role.INCHARGE) {
            throw new IllegalArgumentException("Only incharge can select sewadars");
        }

        // Check max_sewadars limit
        long currentSelections = selectionRepository.countByProgramIdAndStatusNot(
                request.getProgramId(), "DROPPED");
        if (program.getMaxSewadars() != null && 
            currentSelections + request.getSewadarIds().size() > program.getMaxSewadars()) {
            throw new IllegalArgumentException(
                String.format("Cannot select more than %d sewadars. Currently selected: %d, trying to add: %d",
                    program.getMaxSewadars(), currentSelections, request.getSewadarIds().size()));
        }

        return request.getSewadarIds().stream().map(sewadarId -> {
            Sewadar sewadar = sewadarRepository.findById(sewadarId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", sewadarId));

            // Check if already selected (but allow if DROPPED)
            selectionRepository.findByProgramIdAndSewadarId(request.getProgramId(), sewadarId)
                    .ifPresent(existing -> {
                        if (!"DROPPED".equals(existing.getStatus())) {
                            throw new IllegalArgumentException("Sewadar already selected for this program");
                        }
                        // If DROPPED, we'll create a new selection (or could update existing, but creating new is cleaner)
                    });

            // Calculate priority score
            Integer priorityScore = calculatePriorityScore(sewadar);

            ProgramSelection selection = ProgramSelection.builder()
                    .program(program)
                    .sewadar(sewadar)
                    .selectedBy(incharge)
                    .status("SELECTED")
                    .priorityScore(priorityScore)
                    .selectionReason(request.getSelectionReason())
                    .build();

            ProgramSelection saved = selectionRepository.save(selection);
            log.info("Sewadar {} selected for program {}", sewadarId, request.getProgramId());
            
            // Notify sewadar via WhatsApp that they have been selected
            String message = String.format("You have been selected for program '%s'. Please check the application for details and actions.", 
                    program.getTitle());
            if (sewadar.getMobile() != null) {
                whatsAppService.sendMessage(sewadar.getMobile(), message);
                log.info("Selection notification sent to sewadar {} via WhatsApp", sewadarId);
            }
            
            return mapToResponse(saved);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramSelectionResponse> getSelectionsByProgram(Long programId) {
        log.info("Fetching selections for program: {}", programId);
        return selectionRepository.findByProgramIdOrderByPriority(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramSelectionResponse> getSelectionsBySewadar(Long sewadarId) {
        log.info("Fetching selections for sewadar: {}", sewadarId);
        // Include DROPPED for sewadar's own view (they should see their dropped selections)
        return selectionRepository.findBySewadarId(sewadarId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SewadarResponse> getPrioritizedSewadars(Long programId, String sortBy) {
        log.info("Getting prioritized sewadars for program {} sorted by {}", programId, sortBy);
        
        // Get all applications for this program
        List<Long> applicantIds = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId))
                .getApplications().stream()
                .filter(app -> "PENDING".equals(app.getStatus()) || "APPROVED".equals(app.getStatus()))
                .map(app -> app.getSewadar().getId())
                .collect(Collectors.toList());

        List<Sewadar> applicants = sewadarRepository.findAllById(applicantIds);

        // Sort based on criteria
        if ("attendance".equalsIgnoreCase(sortBy)) {
            applicants.sort((a, b) -> {
                Long aCount = (long) attendanceRepository.findAttendedBySewadarId(a.getId()).size();
                Long bCount = (long) attendanceRepository.findAttendedBySewadarId(b.getId()).size();
                return bCount.compareTo(aCount);
            });
        } else if ("joiningDate".equalsIgnoreCase(sortBy)) {
            applicants.sort((a, b) -> {
                if (a.getJoiningDate() == null) return 1;
                if (b.getJoiningDate() == null) return -1;
                return a.getJoiningDate().compareTo(b.getJoiningDate());
            });
        } else if ("profession".equalsIgnoreCase(sortBy)) {
            applicants.sort((a, b) -> {
                String aProf = a.getProfession() != null ? a.getProfession() : "";
                String bProf = b.getProfession() != null ? b.getProfession() : "";
                return aProf.compareTo(bProf);
            });
        }

        return applicants.stream()
                .map(this::mapSewadarToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramSelectionResponse updateSelectionStatus(Long id, String status) {
        log.info("Updating selection {} status to {}", id, status);
        ProgramSelection selection = selectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramSelection", "id", id));

        selection.setStatus(status);
        ProgramSelection updated = selectionRepository.save(selection);
        return mapToResponse(updated);
    }

    @Override
    public void removeSelection(Long id) {
        log.info("Removing selection: {}", id);
        ProgramSelection selection = selectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProgramSelection", "id", id));
        selectionRepository.delete(selection);
    }

    private Integer calculatePriorityScore(Sewadar sewadar) {
        int score = 0;
        
        // Attendance history (higher is better)
        Long attendanceCount = (long) attendanceRepository.findAttendedBySewadarId(sewadar.getId()).size();
        score += attendanceCount.intValue() * 10;

        // Joining date (earlier is better)
        if (sewadar.getJoiningDate() != null) {
            long daysSinceJoining = java.time.temporal.ChronoUnit.DAYS.between(
                    sewadar.getJoiningDate(), java.time.LocalDate.now());
            score += (int) (365 - daysSinceJoining) / 10; // More days = lower score
        }

        return score;
    }

    private ProgramSelectionResponse mapToResponse(ProgramSelection selection) {
        SewadarResponse sewadar = mapSewadarToResponse(selection.getSewadar());
        SewadarResponse selectedBy = mapSewadarToResponse(selection.getSelectedBy());

        return ProgramSelectionResponse.builder()
                .id(selection.getId())
                .programId(selection.getProgram().getId())
                .programTitle(selection.getProgram().getTitle())
                .sewadar(sewadar)
                .selectedBy(selectedBy)
                .selectedAt(selection.getSelectedAt())
                .status(selection.getStatus())
                .priorityScore(selection.getPriorityScore())
                .selectionReason(selection.getSelectionReason())
                .build();
    }

    private SewadarResponse mapSewadarToResponse(Sewadar sewadar) {
        return SewadarResponse.builder()
                .id(sewadar.getId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .dept(sewadar.getDept())
                .profession(sewadar.getProfession())
                .joiningDate(sewadar.getJoiningDate())
                .role(sewadar.getRole() != null ? sewadar.getRole().name() : "SEWADAR")
                .build();
    }
}

