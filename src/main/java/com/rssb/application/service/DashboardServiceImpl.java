package com.rssb.application.service;

import com.rssb.application.dto.*;
import com.rssb.application.entity.*;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final SewadarRepository sewadarRepository;
    private final AttendanceRepository attendanceRepository;
    private final ProgramRepository programRepository;
    private final ProgramApplicationRepository applicationRepository;
    private final ProgramDateRepository programDateRepository;

    @Override
    public SewadarDashboardResponse getSewadars(DashboardQueryRequest request, String currentUserId, String currentUserRole) {
        log.info("Dashboard: Getting sewadars with filters - page: {}, size: {}", request.getPage(), request.getSize());
        log.info("Dashboard filters - location: {}, languages: {}, languageMatchType: {}, joiningDateFrom: {}, joiningDateTo: {}", 
                request.getLocation(), request.getLanguages(), request.getLanguageMatchType(), 
                request.getJoiningDateFrom(), request.getJoiningDateTo());
        
        // Build specification for filtering
        Specification<Sewadar> spec = buildSewadarSpecification(request, currentUserId, currentUserRole);
        
        // For attendance-based sorting, we need to fetch all, sort in memory, then paginate
        boolean needsMemorySort = isAttendanceBasedSort(request.getSortBy());
        
        List<SewadarDashboardResponse.SewadarDashboardItem> items;
        long totalElements;
        int totalPages;
        
        if (needsMemorySort) {
            // Fetch all matching sewadars
            List<Sewadar> allSewadars = sewadarRepository.findAll(spec);
            
            // Map to items with attendance stats
            items = allSewadars.stream()
                    .map(this::mapToSewadarDashboardItem)
                    .collect(Collectors.toList());
            
            // Sort in memory
            items = sortSewadarItems(items, request.getSortBy(), request.getSortOrder());
            
            // Manual pagination
            totalElements = items.size();
            totalPages = (int) Math.ceil((double) totalElements / request.getSize());
            int start = request.getPage() * request.getSize();
            int end = Math.min(start + request.getSize(), items.size());
            items = items.subList(start, end);
        } else {
            // Use database sorting
            Sort sort = buildSewadarSort(request);
            Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
            Page<Sewadar> sewadarPage = sewadarRepository.findAll(spec, pageable);
            
            items = sewadarPage.getContent().stream()
                    .map(this::mapToSewadarDashboardItem)
                    .collect(Collectors.toList());
            
            totalElements = sewadarPage.getTotalElements();
            totalPages = sewadarPage.getTotalPages();
        }
        
        return SewadarDashboardResponse.builder()
                .sewadars(items)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .pageSize(request.getSize())
                .build();
    }
    
    private boolean isAttendanceBasedSort(String sortBy) {
        if (sortBy == null) return false;
        String lower = sortBy.toLowerCase();
        return lower.contains("program") || lower.contains("day") || lower.contains("beas") || lower.contains("nonbeas");
    }
    
    private List<SewadarDashboardResponse.SewadarDashboardItem> sortSewadarItems(
            List<SewadarDashboardResponse.SewadarDashboardItem> items, String sortBy, String sortOrder) {
        if (sortBy == null) return items;
        
        Comparator<SewadarDashboardResponse.SewadarDashboardItem> comparator = null;
        String lower = sortBy.toLowerCase();
        
        if (lower.contains("totalprogram") || lower.contains("totalprograms")) {
            comparator = Comparator.comparing(SewadarDashboardResponse.SewadarDashboardItem::getTotalProgramsCount);
        } else if (lower.contains("totalday") || lower.contains("totaldays")) {
            comparator = Comparator.comparing(SewadarDashboardResponse.SewadarDashboardItem::getTotalDaysAttended);
        } else if (lower.contains("beasday") || lower.contains("beasdays")) {
            comparator = Comparator.comparing(SewadarDashboardResponse.SewadarDashboardItem::getBeasDaysAttended);
        } else if (lower.contains("nonbeasday") || lower.contains("nonbeasdays")) {
            comparator = Comparator.comparing(SewadarDashboardResponse.SewadarDashboardItem::getNonBeasDaysAttended);
        }
        
        if (comparator != null) {
            if ("DESC".equalsIgnoreCase(sortOrder)) {
                comparator = comparator.reversed();
            }
            items.sort(comparator);
        }
        
        return items;
    }

    @Override
    public SewadarDetailedAttendanceResponse getSewadarDetailedAttendance(String sewadarId, String currentUserId, String currentUserRole) {
        log.info("Dashboard: Getting detailed attendance for sewadar: {}", sewadarId);
        
        // Check access: SEWADAR can only see their own data
        // INCHARGE and ADMIN can see any sewadar's data
        if ("SEWADAR".equals(currentUserRole) && !sewadarId.equals(currentUserId)) {
            throw new IllegalArgumentException("Sewadars can only view their own attendance");
        }
        
        Sewadar sewadar = sewadarRepository.findByZonalId(sewadarId)
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "zonal_id", sewadarId));
        
        // Get all attendance records for this sewadar
        List<Attendance> attendances = attendanceRepository.findBySewadarZonalId(sewadarId);
        
        // Map to detailed records (one per program-date)
        List<SewadarDetailedAttendanceResponse.AttendanceRecord> records = attendances.stream()
                .map(att -> {
                    Program program = att.getProgram();
                    LocalDate attendanceDate = att.getProgramDate().getProgramDate();
                    
                    return SewadarDetailedAttendanceResponse.AttendanceRecord.builder()
                            .programId(program.getId())
                            .programTitle(program.getTitle())
                            .programLocation(program.getLocation())
                            .attendanceDate(attendanceDate)
                            .status("Present")
                            .build();
                })
                .sorted(Comparator.comparing(SewadarDetailedAttendanceResponse.AttendanceRecord::getAttendanceDate)
                        .thenComparing(SewadarDetailedAttendanceResponse.AttendanceRecord::getProgramTitle))
                .collect(Collectors.toList());
        
        return SewadarDetailedAttendanceResponse.builder()
                .sewadarId(sewadar.getZonalId())
                .sewadarName(sewadar.getFirstName() + " " + sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .records(records)
                .totalRecords((long) records.size())
                .build();
    }

    @Override
    public ProgramDetailedAttendanceResponse getProgramDetailedAttendance(Long programId) {
        log.info("Dashboard: Getting detailed attendance for program: {}", programId);
        
        Program program = programRepository.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));
        
        // Get all program dates (fixed set)
        List<ProgramDate> programDates = programDateRepository.findByProgramIdOrderByProgramDateAsc(programId);
        List<LocalDate> dates = programDates.stream()
                .map(ProgramDate::getProgramDate)
                .collect(Collectors.toList());
        
        // Get all approved sewadars for this program
        List<ProgramAttendeeResponse> approvedAttendees = getApprovedAttendeesForProgram(programId);
        
        // Get all attendance records for this program
        List<Attendance> attendances = attendanceRepository.findByProgramId(programId);
        
        // Create a map: sewadarId -> date -> status
        Map<String, Map<LocalDate, String>> sewadarDateStatusMap = new HashMap<>();
        for (Attendance att : attendances) {
            String sewadarId = att.getSewadar().getZonalId();
            LocalDate attDate = att.getProgramDate().getProgramDate();
            sewadarDateStatusMap.computeIfAbsent(sewadarId, k -> new HashMap<>())
                    .put(attDate, "Present");
        }
        
        // Build rows: one per approved sewadar
        List<ProgramDetailedAttendanceResponse.SewadarAttendanceRow> rows = approvedAttendees.stream()
                .map(attendee -> {
                    Map<LocalDate, String> dateStatusMap = sewadarDateStatusMap.getOrDefault(
                            attendee.getZonalId(), new HashMap<>());
                    
                    // Fill in all program dates (Present or Absent)
                    Map<LocalDate, String> fullDateStatusMap = new HashMap<>();
                    for (LocalDate date : dates) {
                        fullDateStatusMap.put(date, dateStatusMap.getOrDefault(date, "Absent"));
                    }
                    
                    return ProgramDetailedAttendanceResponse.SewadarAttendanceRow.builder()
                            .zonalId(attendee.getZonalId())
                            .sewadarName(attendee.getFirstName() + " " + attendee.getLastName())
                            .mobile(attendee.getMobile())
                            .dateStatusMap(fullDateStatusMap)
                            .build();
                })
                .collect(Collectors.toList());
        
        return ProgramDetailedAttendanceResponse.builder()
                .programId(program.getId())
                .programTitle(program.getTitle())
                .programDates(dates)
                .sewadarRows(rows)
                .totalSewadars((long) rows.size())
                .build();
    }

    @Override
    public ApplicationDashboardResponse getApplications(DashboardQueryRequest request, String currentUserId, String currentUserRole) {
        log.info("Dashboard: Getting applications with filters - page: {}, size: {}", request.getPage(), request.getSize());
        
        // Build specification for filtering
        Specification<ProgramApplication> spec = buildApplicationSpecification(request, currentUserId, currentUserRole);
        
        // Create pageable
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        
        // Query with pagination
        Page<ProgramApplication> applicationPage = applicationRepository.findAll(spec, pageable);
        
        // Map to response
        List<ApplicationDashboardResponse.ApplicationDashboardItem> items = applicationPage.getContent().stream()
                .map(app -> ApplicationDashboardResponse.ApplicationDashboardItem.builder()
                        .applicationId(app.getId())
                        .sewadarZonalId(app.getSewadar().getZonalId())
                        .sewadarName(app.getSewadar().getFirstName() + " " + app.getSewadar().getLastName())
                        .mobile(app.getSewadar().getMobile())
                        .status(app.getStatus())
                        .appliedAt(app.getAppliedAt())
                        .build())
                .collect(Collectors.toList());
        
        return ApplicationDashboardResponse.builder()
                .applications(items)
                .totalElements(applicationPage.getTotalElements())
                .totalPages(applicationPage.getTotalPages())
                .currentPage(applicationPage.getNumber())
                .pageSize(applicationPage.getSize())
                .build();
    }

    // Helper methods
    private Specification<Sewadar> buildSewadarSpecification(DashboardQueryRequest request, String currentUserId, String currentUserRole) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Role-based access: SEWADAR can only see themselves
            // INCHARGE and ADMIN can see all sewadars (no filter needed)
            if ("SEWADAR".equals(currentUserRole)) {
                predicates.add(cb.equal(root.get("zonalId"), currentUserId));
            }
            // INCHARGE and ADMIN can see all (no filter needed)
            
            // Location filter - case insensitive
            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("location")), request.getLocation().trim().toUpperCase()));
            }
            
            // Joining date range filter
            if (request.getJoiningDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("joiningDate"), request.getJoiningDateFrom()));
            }
            if (request.getJoiningDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("joiningDate"), request.getJoiningDateTo()));
            }
            
            // Language filter
            if (request.getLanguages() != null && !request.getLanguages().isEmpty()) {
                if ("ALL".equalsIgnoreCase(request.getLanguageMatchType())) {
                    // Must have ALL specified languages - use EXISTS subqueries for each language
                    for (String lang : request.getLanguages()) {
                        Subquery<Long> existsSubquery = query.subquery(Long.class);
                        Root<SewadarLanguage> langRoot = existsSubquery.from(SewadarLanguage.class);
                        existsSubquery.select(cb.literal(1L))
                                .where(cb.and(
                                        cb.equal(langRoot.get("sewadar").get("zonalId"), root.get("zonalId")),
                                        cb.equal(cb.upper(langRoot.get("language")), lang.toUpperCase())
                                ));
                        predicates.add(cb.exists(existsSubquery));
                    }
                } else {
                    // Must have ANY of the specified languages (default) - case insensitive
                    Join<Sewadar, SewadarLanguage> languageJoin = root.join("languages", JoinType.INNER);
                    List<String> upperLanguages = request.getLanguages().stream()
                            .map(String::toUpperCase)
                            .collect(Collectors.toList());
                    Predicate langPredicate = cb.upper(languageJoin.get("language")).in(upperLanguages);
                    predicates.add(langPredicate);
                    // Ensure distinct results when joining with languages
                    query.distinct(true);
                }
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort buildSewadarSort(DashboardQueryRequest request) {
        String sortBy = request.getSortBy();
        String sortOrder = request.getSortOrder() != null ? request.getSortOrder() : "ASC";
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortOrder) ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(Sort.Direction.ASC, "zonalId");
        }
        
        // Handle attendance-based sorting (requires custom logic)
        switch (sortBy.toLowerCase()) {
            case "totalprograms":
            case "totalprogramscount":
                // Sort by total programs attended (requires subquery or post-processing)
                return Sort.by(direction, "zonalId"); // Fallback, will sort in memory
            case "totaldays":
            case "totaldaysattended":
                // Sort by total days (requires subquery or post-processing)
                return Sort.by(direction, "zonalId"); // Fallback, will sort in memory
            case "beasdays":
            case "beasdaysattended":
                // Sort by BEAS days (requires subquery or post-processing)
                return Sort.by(direction, "zonalId"); // Fallback, will sort in memory
            case "nonbeasdays":
            case "nonbeasdaysattended":
                // Sort by non-BEAS days (requires subquery or post-processing)
                return Sort.by(direction, "zonalId"); // Fallback, will sort in memory
            case "joiningdate":
                return Sort.by(direction, "joiningDate");
            default:
                return Sort.by(direction, "zonalId");
        }
    }

    private SewadarDashboardResponse.SewadarDashboardItem mapToSewadarDashboardItem(Sewadar sewadar) {
        String zonalId = sewadar.getZonalId();
        
        // Get attendance statistics
        Long totalPrograms = attendanceRepository.countAttendedProgramsBySewadarId(zonalId);
        Long totalDays = attendanceRepository.countDaysAttendedBySewadarId(zonalId);
        Long beasPrograms = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(zonalId, "BEAS");
        Long beasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(zonalId, "BEAS");
        Long nonBeasPrograms = attendanceRepository.countAttendedProgramsBySewadarIdAndLocationType(zonalId, "NON_BEAS");
        Long nonBeasDays = attendanceRepository.countDaysAttendedBySewadarIdAndLocationType(zonalId, "NON_BEAS");
        
        // Get languages
        List<String> languages = sewadar.getLanguages().stream()
                .map(SewadarLanguage::getLanguage)
                .collect(Collectors.toList());
        
        return SewadarDashboardResponse.SewadarDashboardItem.builder()
                .zonalId(sewadar.getZonalId())
                .firstName(sewadar.getFirstName())
                .lastName(sewadar.getLastName())
                .mobile(sewadar.getMobile())
                .location(sewadar.getLocation())
                .profession(sewadar.getProfession())
                .joiningDate(sewadar.getJoiningDate())
                .languages(languages)
                .totalProgramsCount(totalPrograms != null ? totalPrograms : 0L)
                .totalDaysAttended(totalDays != null ? totalDays : 0L)
                .beasProgramsCount(beasPrograms != null ? beasPrograms : 0L)
                .beasDaysAttended(beasDays != null ? beasDays : 0L)
                .nonBeasProgramsCount(nonBeasPrograms != null ? nonBeasPrograms : 0L)
                .nonBeasDaysAttended(nonBeasDays != null ? nonBeasDays : 0L)
                .build();
    }

    private Specification<ProgramApplication> buildApplicationSpecification(DashboardQueryRequest request, String currentUserId, String currentUserRole) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Role-based access: SEWADAR can only see their own applications
            // INCHARGE and ADMIN can see all applications (no filter needed)
            if ("SEWADAR".equals(currentUserRole)) {
                predicates.add(cb.equal(root.get("sewadar").get("zonalId"), currentUserId));
            }
            // INCHARGE and ADMIN can see all applications (no filter needed)
            
            // Program filter
            if (request.getProgramId() != null) {
                predicates.add(cb.equal(root.get("program").get("id"), request.getProgramId()));
            }
            
            // Status filter
            if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(request.getStatuses()));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Helper method to get approved attendees (reuse from AttendanceService)
    private List<ProgramAttendeeResponse> getApprovedAttendeesForProgram(Long programId) {
        List<ProgramApplication> approvedApps = applicationRepository.findByProgramIdAndStatus(programId, "APPROVED");
        return approvedApps.stream()
                .map(app -> ProgramAttendeeResponse.builder()
                        .zonalId(app.getSewadar().getZonalId())
                        .firstName(app.getSewadar().getFirstName())
                        .lastName(app.getSewadar().getLastName())
                        .mobile(app.getSewadar().getMobile())
                        .photoUrl(app.getSewadar().getPhotoUrl())
                        .applicationId(app.getId())
                        .applicationStatus(app.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    // Export methods
    @Override
    public byte[] exportSewadars(DashboardQueryRequest request, String format, String currentUserId, String currentUserRole) {
        log.info("Exporting sewadars in format: {}", format);
        
        // Get data (without pagination for export)
        DashboardQueryRequest exportRequest = DashboardQueryRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE) // Get all records
                .languages(request.getLanguages())
                .languageMatchType(request.getLanguageMatchType())
                .location(request.getLocation())
                .joiningDateFrom(request.getJoiningDateFrom())
                .joiningDateTo(request.getJoiningDateTo())
                .sortBy(request.getSortBy())
                .sortOrder(request.getSortOrder())
                .build();
        
        SewadarDashboardResponse response = getSewadars(exportRequest, currentUserId, currentUserRole);
        
        try {
            return switch (format.toUpperCase()) {
                case "CSV" -> com.rssb.application.util.ExportUtil.exportSewadarsToCSV(response);
                case "XLSX" -> com.rssb.application.util.ExportUtil.exportSewadarsToXLSX(response);
                case "PDF" -> com.rssb.application.util.ExportUtil.exportSewadarsToPDF(response);
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };
        } catch (Exception e) {
            log.error("Error exporting sewadars", e);
            throw new RuntimeException("Failed to export sewadars: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportSewadarAttendance(String sewadarId, String format, String currentUserId, String currentUserRole) {
        log.info("Exporting sewadar attendance in format: {}", format);
        
        SewadarDetailedAttendanceResponse response = getSewadarDetailedAttendance(sewadarId, currentUserId, currentUserRole);
        
        try {
            return switch (format.toUpperCase()) {
                case "CSV" -> com.rssb.application.util.ExportUtil.exportSewadarAttendanceToCSV(response);
                case "XLSX", "PDF" -> {
                    // For now, only CSV is implemented for detailed attendance
                    // Can extend later
                    log.warn("XLSX/PDF export for sewadar attendance not yet implemented, using CSV");
                    yield com.rssb.application.util.ExportUtil.exportSewadarAttendanceToCSV(response);
                }
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };
        } catch (Exception e) {
            log.error("Error exporting sewadar attendance", e);
            throw new RuntimeException("Failed to export attendance: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportProgramAttendance(Long programId, String format) {
        log.info("Exporting program attendance in format: {}", format);
        
        ProgramDetailedAttendanceResponse response = getProgramDetailedAttendance(programId);
        
        try {
            return switch (format.toUpperCase()) {
                case "CSV" -> com.rssb.application.util.ExportUtil.exportProgramAttendanceToCSV(response);
                case "XLSX", "PDF" -> {
                    // For now, only CSV is implemented for program attendance
                    log.warn("XLSX/PDF export for program attendance not yet implemented, using CSV");
                    yield com.rssb.application.util.ExportUtil.exportProgramAttendanceToCSV(response);
                }
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };
        } catch (Exception e) {
            log.error("Error exporting program attendance", e);
            throw new RuntimeException("Failed to export attendance: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] exportApplications(DashboardQueryRequest request, String format, String currentUserId, String currentUserRole) {
        log.info("Exporting applications in format: {}", format);
        
        // Get data (without pagination for export)
        DashboardQueryRequest exportRequest = DashboardQueryRequest.builder()
                .page(0)
                .size(Integer.MAX_VALUE) // Get all records
                .programId(request.getProgramId())
                .statuses(request.getStatuses())
                .build();
        
        ApplicationDashboardResponse response = getApplications(exportRequest, currentUserId, currentUserRole);
        
        try {
            return switch (format.toUpperCase()) {
                case "CSV" -> com.rssb.application.util.ExportUtil.exportApplicationsToCSV(response);
                case "XLSX", "PDF" -> {
                    // For now, only CSV is implemented for applications
                    log.warn("XLSX/PDF export for applications not yet implemented, using CSV");
                    yield com.rssb.application.util.ExportUtil.exportApplicationsToCSV(response);
                }
                default -> throw new IllegalArgumentException("Unsupported format: " + format);
            };
        } catch (Exception e) {
            log.error("Error exporting applications", e);
            throw new RuntimeException("Failed to export applications: " + e.getMessage(), e);
        }
    }
}

