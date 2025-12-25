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
    private final com.rssb.application.repository.AttendanceRepository attendanceRepository;

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

        // Update program dates intelligently to preserve referential integrity
        if (request.getProgramDates() != null) {
            List<com.rssb.application.entity.ProgramDate> oldProgramDates = 
                    programDateRepository.findByProgramId(id);
            List<java.time.LocalDate> newDates = request.getProgramDates();
            
            // Map old dates by their date value for quick lookup
            java.util.Map<java.time.LocalDate, com.rssb.application.entity.ProgramDate> oldDatesMap = 
                    oldProgramDates.stream()
                            .collect(Collectors.toMap(
                                    ProgramDate::getProgramDate,
                                    pd -> pd,
                                    (existing, replacement) -> existing // Keep first if duplicates
                            ));
            
            // 1. DELETE: Remove ProgramDate entities for dates that are no longer in the new list
            // This will cascade delete attendance records (or we handle manually)
            List<com.rssb.application.entity.ProgramDate> toDelete = oldProgramDates.stream()
                    .filter(pd -> !newDates.contains(pd.getProgramDate()))
                    .collect(Collectors.toList());
            
            if (!toDelete.isEmpty()) {
                log.info("Removing {} program dates that are no longer in the program", toDelete.size());
                for (com.rssb.application.entity.ProgramDate pdToDelete : toDelete) {
                    // Delete attendance records first (if not using CASCADE)
                    List<com.rssb.application.entity.Attendance> attendancesToDelete = 
                            attendanceRepository.findByProgramDateId(pdToDelete.getId());
                    if (!attendancesToDelete.isEmpty()) {
                        attendanceRepository.deleteAll(attendancesToDelete);
                        log.info("Deleted {} attendance records for removed program_date_id {} (date: {})", 
                                attendancesToDelete.size(), pdToDelete.getId(), pdToDelete.getProgramDate());
                    }
                    programDateRepository.delete(pdToDelete);
                }
            }
            
            // 2. UPDATE/PRESERVE: For dates that exist in both old and new lists, keep the ProgramDate entity
            // This preserves the ID and maintains foreign key relationships with attendance records
            List<java.time.LocalDate> datesToPreserve = newDates.stream()
                    .filter(oldDatesMap::containsKey)
                    .collect(Collectors.toList());
            
            // 3. CREATE: For dates that are new (not in old list), create new ProgramDate entities
            List<java.time.LocalDate> datesToCreate = newDates.stream()
                    .filter(date -> !oldDatesMap.containsKey(date))
                    .collect(Collectors.toList());
            
            if (!datesToCreate.isEmpty()) {
                log.info("Creating {} new program dates", datesToCreate.size());
                for (java.time.LocalDate date : datesToCreate) {
                    ProgramDate programDate = ProgramDate.builder()
                            .program(program)
                            .programDate(date)
                            .status("SCHEDULED")
                            .build();
                    programDateRepository.save(programDate);
                }
            }
            
            log.info("Program dates updated: {} preserved, {} created, {} deleted", 
                    datesToPreserve.size(), datesToCreate.size(), toDelete.size());
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

