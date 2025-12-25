package com.rssb.application.service;

import com.rssb.application.dto.ProgramRequest;
import com.rssb.application.dto.ProgramResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.entity.ProgramDate;
import com.rssb.application.repository.ProgramApplicationRepository;
import com.rssb.application.repository.ProgramDateRepository;
import com.rssb.application.repository.ProgramRepository;
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
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;
    private final ProgramApplicationRepository applicationRepository;
    private final ProgramDateRepository programDateRepository;

    @Override
    public ProgramResponse createProgram(ProgramRequest request) {
        log.info("Creating program: {}", request.getTitle());
        
        Sewadar incharge = sewadarRepository.findByZonalId(request.getCreatedById())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", request.getCreatedById()));

        Program program = Program.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .status(request.getStatus() != null ? request.getStatus() : "scheduled")
                .maxSewadars(request.getMaxSewadars())
                .createdBy(incharge)
                .build();

        Program saved = programRepository.save(program);

        // Create program dates
        if (request.getProgramDates() != null && !request.getProgramDates().isEmpty()) {
            for (java.time.LocalDate date : request.getProgramDates()) {
                ProgramDate programDate = ProgramDate.builder()
                        .program(saved)
                        .programDate(date)
                        .status("SCHEDULED")
                        .build();
                programDateRepository.save(programDate);
            }
        }
        log.info("Program created with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponse getProgramById(Long id) {
        log.info("Fetching program with id: {}", id);
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", id));
        return mapToResponse(program);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> getAllPrograms() {
        log.info("Fetching all programs");
        return programRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> getProgramsByIncharge(Long inchargeZonalId) {
        log.info("Fetching programs for incharge: {}", inchargeZonalId);
        return programRepository.findByCreatedByZonalId(inchargeZonalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProgramResponse updateProgram(Long id, ProgramRequest request) {
        log.info("Updating program with id: {}", id);
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", id));

        program.setTitle(request.getTitle());
        program.setDescription(request.getDescription());
        program.setLocation(request.getLocation());
        if (request.getStatus() != null) {
            program.setStatus(request.getStatus());
        }
        program.setMaxSewadars(request.getMaxSewadars());

        // Update program dates
        if (request.getProgramDates() != null) {
            // Delete existing dates
            programDateRepository.findByProgramId(id).forEach(programDateRepository::delete);
            
            // Create new dates
            for (java.time.LocalDate date : request.getProgramDates()) {
                ProgramDate programDate = ProgramDate.builder()
                        .program(program)
                        .programDate(date)
                        .status("SCHEDULED")
                        .build();
                programDateRepository.save(programDate);
            }
        }

        Program updated = programRepository.save(program);
        log.info("Program updated with id: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public void deleteProgram(Long id) {
        log.info("Deleting program with id: {}", id);
        Program program = programRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", id));
        programRepository.delete(program);
        log.info("Program deleted with id: {}", id);
    }

    private ProgramResponse mapToResponse(Program program) {
        SewadarResponse createdBy = SewadarResponse.builder()
                .zonalId(program.getCreatedBy().getZonalId())
                .firstName(program.getCreatedBy().getFirstName())
                .lastName(program.getCreatedBy().getLastName())
                .build();

        // Count only active applications (exclude DROPPED)
        Long applicationCount = applicationRepository.findByProgramIdAndStatusNot(program.getId(), "DROPPED").stream().count();

        // Get program dates
        List<java.time.LocalDate> dates = programDateRepository.findByProgramIdOrderByProgramDateAsc(program.getId())
                .stream()
                .map(ProgramDate::getProgramDate)
                .collect(java.util.stream.Collectors.toList());

        // Derive locationType from location
        String locationType = "BEAS".equalsIgnoreCase(program.getLocation()) ? "BEAS" : "NON_BEAS";

        return ProgramResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .description(program.getDescription())
                .location(program.getLocation())
                .locationType(locationType)
                .programDates(dates)
                .status(program.getStatus())
                .maxSewadars(program.getMaxSewadars())
                .createdBy(createdBy)
                .applicationCount(applicationCount)
                .build();
    }
}

