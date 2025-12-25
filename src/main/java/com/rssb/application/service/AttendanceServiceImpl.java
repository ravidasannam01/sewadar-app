package com.rssb.application.service;

import com.rssb.application.dto.AllSewadarsAttendanceSummaryResponse;
import com.rssb.application.dto.AttendanceResponse;
import com.rssb.application.dto.AttendanceRequest;
import com.rssb.application.dto.SewadarAttendanceSummaryResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Attendance;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.Role;
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
    public List<AttendanceResponse> markAttendance(AttendanceRequest request) {
        log.info("Incharge {} marking attendance for program {}", request.getMarkedById(), request.getProgramId());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        Sewadar incharge = sewadarRepository.findByZonalId(request.getMarkedById())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", request.getMarkedById()));

        if (incharge.getRole() != Role.INCHARGE) {
            throw new IllegalArgumentException("Only incharge can mark attendance");
        }

        return request.getSewadarIds().stream().map(sewadarZonalId -> {
            Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));

            // Check if attendance already exists
            Attendance existing = attendanceRepository.findByProgramIdAndSewadarZonalId(request.getProgramId(), sewadarZonalId)
                    .orElse(null);

            Attendance attendance;
            if (existing != null) {
                // Update existing
                existing.setAttended(true);
                existing.setDaysParticipated(request.getDaysParticipated());
                existing.setNotes(request.getNotes());
                existing.setMarkedAt(LocalDateTime.now());
                attendance = attendanceRepository.save(existing);
            } else {
                // Create new
                attendance = Attendance.builder()
                        .program(program)
                        .sewadar(sewadar)
                        .attended(true)
                        .markedBy(incharge.getZonalId())
                        .daysParticipated(request.getDaysParticipated())
                        .notes(request.getNotes())
                        .build();
                attendance = attendanceRepository.save(attendance);
            }

            log.info("Attendance marked for sewadar {} in program {}", sewadarZonalId, request.getProgramId());
            return mapToResponse(attendance);
        }).collect(Collectors.toList());
    }

    @Override
    public AttendanceResponse updateAttendance(Long id, Boolean attended, Integer daysParticipated, String notes) {
        log.info("Updating attendance: {}", id);
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", "id", id));

        if (attended != null) {
            attendance.setAttended(attended);
        }
        if (daysParticipated != null) {
            attendance.setDaysParticipated(daysParticipated);
        }
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
    public List<AttendanceResponse> getAttendanceBySewadar(Long sewadarZonalId) {
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
    public SewadarAttendanceSummaryResponse getSewadarAttendanceSummary(Long sewadarZonalId) {
        log.info("Fetching attendance summary for sewadar: {}", sewadarZonalId);
        
        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarZonalId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarZonalId));
        
        // Get all attended programs
        List<Attendance> allAttendances = attendanceRepository.findAttendedBySewadarId(sewadarZonalId);
        
        // Separate BEAS and non-BEAS (derive from location)
        List<SewadarAttendanceSummaryResponse.AttendanceDetail> beasAttendances = allAttendances.stream()
                .filter(a -> "BEAS".equalsIgnoreCase(a.getProgram().getLocation()))
                .map(this::mapToAttendanceDetail)
                .collect(Collectors.toList());
        
        List<SewadarAttendanceSummaryResponse.AttendanceDetail> nonBeasAttendances = allAttendances.stream()
                .filter(a -> !"BEAS".equalsIgnoreCase(a.getProgram().getLocation()))
                .map(this::mapToAttendanceDetail)
                .collect(Collectors.toList());
        
        // Calculate counts
        Long beasProgramsCount = (long) beasAttendances.size();
        Long nonBeasProgramsCount = (long) nonBeasAttendances.size();
        Long totalProgramsCount = (long) allAttendances.size();
        
        // Calculate days
        Integer beasDays = beasAttendances.stream()
                .mapToInt(a -> a.getDaysParticipated() != null ? a.getDaysParticipated() : 0)
                .sum();
        Integer nonBeasDays = nonBeasAttendances.stream()
                .mapToInt(a -> a.getDaysParticipated() != null ? a.getDaysParticipated() : 0)
                .sum();
        Integer totalDays = beasDays + nonBeasDays;
        
        return SewadarAttendanceSummaryResponse.builder()
                .sewadarId(sewadar.getZonalId())
                .sewadarName(sewadar.getFirstName() + " " + sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .beasProgramsCount(beasProgramsCount)
                .beasDaysAttended(beasDays)
                .beasAttendances(beasAttendances)
                .nonBeasProgramsCount(nonBeasProgramsCount)
                .nonBeasDaysAttended(nonBeasDays)
                .nonBeasAttendances(nonBeasAttendances)
                .totalProgramsCount(totalProgramsCount)
                .totalDaysAttended(totalDays)
                .build();
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
                    
                    Integer beasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "BEAS");
                    Integer nonBeasDays = attendanceRepository.sumDaysAttendedBySewadarIdAndLocationType(
                            sewadar.getZonalId(), "NON_BEAS");
                    Integer totalDays = attendanceRepository.sumDaysAttendedBySewadarId(sewadar.getZonalId());
                    
                    return AllSewadarsAttendanceSummaryResponse.SewadarSummary.builder()
                            .sewadarId(sewadar.getZonalId())
                            .sewadarName(sewadar.getFirstName() + " " + sewadar.getLastName())
                            .mobile(sewadar.getMobile())
                            .beasProgramsCount(beasCount)
                            .beasDaysAttended(beasDays != null ? beasDays : 0)
                            .nonBeasProgramsCount(nonBeasCount)
                            .nonBeasDaysAttended(nonBeasDays != null ? nonBeasDays : 0)
                            .totalProgramsCount(totalCount)
                            .totalDaysAttended(totalDays != null ? totalDays : 0)
                            .build();
                })
                .collect(Collectors.toList());
        
        return AllSewadarsAttendanceSummaryResponse.builder()
                .sewadars(summaries)
                .build();
    }
    
    private SewadarAttendanceSummaryResponse.AttendanceDetail mapToAttendanceDetail(Attendance attendance) {
        // Derive locationType from location
        String locationType = "BEAS".equalsIgnoreCase(attendance.getProgram().getLocation()) ? "BEAS" : "NON_BEAS";
        return SewadarAttendanceSummaryResponse.AttendanceDetail.builder()
                .programId(attendance.getProgram().getId())
                .programTitle(attendance.getProgram().getTitle())
                .location(attendance.getProgram().getLocation())
                .locationType(locationType)
                .attended(attendance.getAttended())
                .daysParticipated(attendance.getDaysParticipated())
                .markedAt(attendance.getMarkedAt())
                .build();
    }

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
                .attended(attendance.getAttended())
                .markedBy(attendance.getMarkedBy())
                .markedAt(attendance.getMarkedAt())
                .notes(attendance.getNotes())
                .daysParticipated(attendance.getDaysParticipated())
                .build();
    }
}

