package com.rssb.application.service;

import com.rssb.application.dto.AllSewadarsAttendanceSummaryResponse;
import com.rssb.application.dto.AttendanceResponse;
import com.rssb.application.dto.AttendanceRequest;
import com.rssb.application.dto.ProgramAttendeeResponse;
import com.rssb.application.dto.SewadarAttendanceSummaryResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Attendance;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramDate;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.AttendanceRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;

    @Override
    public List<AttendanceResponse> markAttendance(AttendanceRequest request, String inchargeZonalId) {
        log.info("Incharge {} marking attendance for program {} on date {}", inchargeZonalId, request.getProgramId(), request.getProgramDate());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        // Validate user role and permission
        Sewadar incharge = sewadarRepository.findByZonalId(inchargeZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", inchargeZonalId));
        
        // ADMIN can mark attendance for any program, INCHARGE only for their own programs
        if (!com.rssb.application.util.PermissionUtil.canManageProgram(incharge, program)) {
            throw new IllegalArgumentException("Only the program creator (or ADMIN) can mark attendance");
        }
        
        if (!com.rssb.application.util.PermissionUtil.hasInchargePermission(incharge)) {
            throw new IllegalArgumentException("Only incharge or admin can mark attendance");
        }

        // Find the ProgramDate entity for the given date (ensures referential integrity)
        ProgramDate programDate = program.getProgramDates().stream()
                .filter(pd -> pd.getProgramDate().equals(request.getProgramDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Date " + request.getProgramDate() + " is not a valid program date. Valid dates: " + 
                        program.getProgramDates().stream().map(pd -> pd.getProgramDate()).toList()));
        
        // Optional: Validate that attendance cannot be marked for future dates
        // (Allow past dates in case incharge forgot to mark on the actual day)
        java.time.LocalDate today = java.time.LocalDate.now();
        if (request.getProgramDate().isAfter(today)) {
            throw new IllegalArgumentException(
                    "Cannot mark attendance for future dates. " +
                    "Requested date: " + request.getProgramDate() + ", Today: " + today);
        }

        return request.getSewadarIds().stream().map(sewadarZonalId -> {
            Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));

            // Verify sewadar is approved for this program
            boolean isApproved = program.getApplications().stream()
                    .anyMatch(app -> app.getSewadar().getZonalId().equals(sewadarZonalId) 
                            && "APPROVED".equals(app.getStatus()));
            
            if (!isApproved) {
                throw new IllegalArgumentException("Sewadar " + sewadarZonalId + " is not approved for this program");
            }

            // Check if attendance already exists for this sewadar-program-date combination
            // Unique constraint on (program_date_id, sewadar_id) prevents duplicates
            Attendance existing = attendanceRepository.findByProgramDateIdAndSewadarZonalId(
                    programDate.getId(), sewadarZonalId)
                    .orElse(null);

            Attendance attendance;
            if (existing != null) {
                // Update existing (if notes changed, etc.)
                if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
                    existing.setNotes(request.getNotes());
                }
                existing.setMarkedAt(LocalDateTime.now());
                attendance = attendanceRepository.save(existing);
            } else {
                // Create new attendance record for this date
                attendance = Attendance.builder()
                        .program(program)
                        .sewadar(sewadar)
                        .programDate(programDate) // Foreign key reference - ensures referential integrity
                        .markedAt(LocalDateTime.now())
                        .notes(request.getNotes())
                        .build();
                attendance = attendanceRepository.save(attendance);
            }

            log.info("Attendance marked for sewadar {} in program {} on date {}", 
                    sewadarZonalId, request.getProgramId(), request.getProgramDate());
            return mapToResponse(attendance);
        }).collect(Collectors.toList());
    }

    @Override
    public AttendanceResponse updateAttendance(Long id, Boolean attended, Integer daysParticipated, String notes) {
        log.info("Updating attendance: {}", id);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));

        // Note: attended and daysParticipated parameters are legacy - not applicable in normalized approach
        // Only notes can be updated
        if (notes != null) {
            attendance.setNotes(notes);
        }
        attendance.setMarkedAt(LocalDateTime.now());

        Attendance updated = attendanceRepository.save(attendance);
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByProgram(Long programId) {
        log.info("Fetching attendance for program: {}", programId);
        return attendanceRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceBySewadar(String sewadarZonalId) {
        log.info("Fetching attendance for sewadar: {}", sewadarZonalId);
        return attendanceRepository.findBySewadarZonalId(sewadarZonalId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceStatistics(Long programId) {
        log.info("Fetching attendance statistics for program: {}", programId);
        // Return all attendance records with additional stats
        return attendanceRepository.findByProgramId(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SewadarAttendanceSummaryResponse getSewadarAttendanceSummary(String sewadarZonalId) {
        log.info("Fetching attendance summary for sewadar: {}", sewadarZonalId);
        
        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));
        
        // Use SQL aggregations for counts (more efficient)
        Long beasProgramsCount = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(sewadarZonalId, "BEAS");
        Long nonBeasProgramsCount = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(sewadarZonalId, "NON_BEAS");
        Long totalProgramsCount = attendanceRepository.countAttendedProgramsBySewadarId(sewadarZonalId);
        
        // Count days using SQL (count of attendance records)
        Long beasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(sewadarZonalId, "BEAS");
        Long nonBeasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(sewadarZonalId, "NON_BEAS");
        Long totalDays = attendanceRepository.countDaysAttendedBySewadarId(sewadarZonalId);
        
        // Get attendance records by location for detail list
        List<Attendance> beasAttendanceEntities = attendanceRepository.findAttendedBySewadarIdAndLocationType(sewadarZonalId, "BEAS");
        List<Attendance> nonBeasAttendanceEntities = attendanceRepository.findAttendedBySewadarIdAndLocationType(sewadarZonalId, "NON_BEAS");
        
        // Group by program and map to DTOs (one detail per program, with day count)
        List<SewadarAttendanceSummaryResponse.AttendanceDetail> beasAttendances = 
                groupAttendancesByProgram(beasAttendanceEntities);
        
        List<SewadarAttendanceSummaryResponse.AttendanceDetail> nonBeasAttendances = 
                groupAttendancesByProgram(nonBeasAttendanceEntities);
        
        return SewadarAttendanceSummaryResponse.builder()
                .sewadarId(sewadar.getZonalId())
                .sewadarName(sewadar.getFirstName() + " " + sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .beasProgramsCount(beasProgramsCount)
                .beasDaysAttended(beasDays != null ? beasDays.intValue() : 0)
                .beasAttendances(beasAttendances)
                .nonBeasProgramsCount(nonBeasProgramsCount)
                .nonBeasDaysAttended(nonBeasDays != null ? nonBeasDays.intValue() : 0)
                .nonBeasAttendances(nonBeasAttendances)
                .totalProgramsCount(totalProgramsCount)
                .totalDaysAttended(totalDays != null ? totalDays.intValue() : 0)
                .build();
    }
    
    /**
     * Group attendance records by program and create summary details
     * Each program gets one detail entry with the count of days attended
     */
    private List<SewadarAttendanceSummaryResponse.AttendanceDetail> groupAttendancesByProgram(List<Attendance> attendances) {
        return attendances.stream()
                .collect(Collectors.groupingBy(Attendance::getProgram))
                .entrySet().stream()
                .map(entry -> {
                    Program program = entry.getKey();
                    List<Attendance> programAttendances = entry.getValue();
                    String locationType = "BEAS".equalsIgnoreCase(program.getLocation()) ? "BEAS" : "NON_BEAS";
                    
                    return SewadarAttendanceSummaryResponse.AttendanceDetail.builder()
                            .programId(program.getId())
                            .programTitle(program.getTitle())
                            .location(program.getLocation())
                            .locationType(locationType)
                            .attended(true) // If record exists, they attended
                            .daysParticipated(programAttendances.size()) // Count of attendance records = days
                            .markedAt(programAttendances.stream()
                                    .map(Attendance::getMarkedAt)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(LocalDateTime.now()))
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public AllSewadarsAttendanceSummaryResponse getAllSewadarsAttendanceSummary() {
        log.info("Fetching attendance summary for all sewadars");
        
        List<Sewadar> allSewadars = sewadarRepository.findAll();
        
        List<AllSewadarsAttendanceSummaryResponse.SewadarSummary> summaries = allSewadars.stream()
                .map(sewadar -> {
                    Long beasCount = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "BEAS");
                    Long nonBeasCount = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "NON_BEAS");
                    Long totalCount = attendanceRepository.countAttendedProgramsBySewadarId(sewadar.getZonalId());
                    
                    // Use SQL aggregations for efficient counting
                    Long beasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "BEAS");
                    Long nonBeasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "NON_BEAS");
                    Long totalDays = attendanceRepository.countDaysAttendedBySewadarId(sewadar.getZonalId());
                    
                    return AllSewadarsAttendanceSummaryResponse.SewadarSummary.builder()
                            .sewadarId(sewadar.getZonalId())
                            .sewadarName(sewadar.getFirstName() + " " + sewadar.getLastName())
                            .mobile(sewadar.getMobile())
                            .beasProgramsCount(beasCount)
                            .beasDaysAttended(beasDays != null ? beasDays.intValue() : 0)
                            .nonBeasProgramsCount(nonBeasCount)
                            .nonBeasDaysAttended(nonBeasDays != null ? nonBeasDays.intValue() : 0)
                            .totalProgramsCount(totalCount)
                            .totalDaysAttended(totalDays != null ? totalDays.intValue() : 0)
                            .build();
                })
                .collect(Collectors.toList());
        
        return AllSewadarsAttendanceSummaryResponse.builder()
                .sewadars(summaries)
                .build();
    }
    
    // mapToAttendanceDetail removed - now using groupAttendancesByProgram for summary

    private AttendanceResponse mapToResponse(Attendance attendance) {
        SewadarResponse sewadar = SewadarResponse.builder()
                .zonalId(attendance.getSewadar().getZonalId())
                .firstName(attendance.getSewadar().getFirstName())
                .lastName(attendance.getSewadar().getLastName())
                .mobile(attendance.getSewadar().getMobile())
                .build();

        return AttendanceResponse.builder()
                .id(attendance.getId())
                .programId(attendance.getProgram().getId())
                .programTitle(attendance.getProgram().getTitle())
                .sewadar(sewadar)
                .attendanceDate(attendance.getProgramDate().getProgramDate()) // Get date from ProgramDate entity
                .programDateId(attendance.getProgramDate().getId())
                .markedAt(attendance.getMarkedAt())
                .notes(attendance.getNotes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramAttendeeResponse> getApprovedAttendeesForProgram(Long programId) {
        log.info("Fetching approved attendees for program: {}", programId);
        
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));
        
        // Get all APPROVED applications for this program
        return program.getApplications().stream()
                .filter(app -> "APPROVED".equals(app.getStatus()))
                .map(app -> {
                    Sewadar sewadar = app.getSewadar();
                    return ProgramAttendeeResponse.builder()
                            .zonalId(sewadar.getZonalId())
                            .firstName(sewadar.getFirstName())
                            .lastName(sewadar.getLastName())
                            .mobile(sewadar.getMobile())
                            .applicationId(app.getId())
                            .applicationStatus(app.getStatus())
                            .build();
                })
                .collect(Collectors.toList());
    }
}

