package com.rssb.application.controller;

import com.rssb.application.dto.AttendanceRequest;
import com.rssb.application.dto.AttendanceResponse;
import com.rssb.application.dto.ProgramAttendeeResponse;
import com.rssb.application.service.AttendanceService;
import com.rssb.application.util.ActionLogger;
import com.rssb.application.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("unused") // REST endpoints are used via HTTP, not direct Java calls
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ActionLogger actionLogger;

    @PostMapping
    public ResponseEntity<List<AttendanceResponse>> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        log.info("POST /api/attendances - Marking attendance");
        
        // Get current user (incharge) from JWT token
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        
        String inchargeZonalId = (String) authentication.getPrincipal();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        List<AttendanceResponse> responses = attendanceService.markAttendance(request, inchargeZonalId);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", request.getProgramId());
        details.put("programDate", request.getProgramDate());
        details.put("sewadarCount", request.getSewadarIds() != null ? request.getSewadarIds().size() : 0);
        details.put("sewadarIds", request.getSewadarIds());
        details.put("attendanceRecordsCreated", responses.size());
        details.put("durationMs", duration);
        actionLogger.logAction("MARK_ATTENDANCE", inchargeZonalId, userRole, details);
        actionLogger.logPerformance("MARK_ATTENDANCE", duration);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /**
     * Get approved sewadars (attendees) for a program
     * @param programId Program ID
     * @return List of approved sewadars who can have attendance marked
     */
    @GetMapping("/program/{programId}/attendees")
    public ResponseEntity<List<ProgramAttendeeResponse>> getApprovedAttendees(@PathVariable Long programId) {
        log.info("GET /api/attendances/program/{}/attendees", programId);
        return ResponseEntity.ok(attendanceService.getApprovedAttendeesForProgram(programId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean attended,
            @RequestParam(required = false) Integer daysParticipated,
            @RequestParam(required = false) String notes) {
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        AttendanceResponse response = attendanceService.updateAttendance(id, attended, daysParticipated, notes);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("attendanceId", id);
        details.put("attended", attended);
        details.put("daysParticipated", daysParticipated);
        details.put("hasNotes", notes != null && !notes.isEmpty());
        details.put("durationMs", duration);
        actionLogger.logAction("UPDATE_ATTENDANCE", userId, userRole, details);
        actionLogger.logPerformance("UPDATE_ATTENDANCE", duration);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByProgram(programId));
    }

    /**
     * Get attendance records by sewadar zonal ID
     * @param sewadarId Sewadar zonal ID
     * @return List of attendance records
     */
    @GetMapping("/sewadar/{sewadarId}")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceBySewadar(@PathVariable String sewadarId) {
        return ResponseEntity.ok(attendanceService.getAttendanceBySewadar(sewadarId));
    }

    @GetMapping("/program/{programId}/statistics")
    public ResponseEntity<List<AttendanceResponse>> getAttendanceStatistics(@PathVariable Long programId) {
        return ResponseEntity.ok(attendanceService.getAttendanceStatistics(programId));
    }

    /**
     * Get attendance summary for a specific sewadar with BEAS/non-BEAS breakdown
     * @param sewadarId Sewadar zonal ID
     */
    @GetMapping("/sewadar/{sewadarId}/summary")
    public ResponseEntity<com.rssb.application.dto.SewadarAttendanceSummaryResponse> getSewadarAttendanceSummary(
            @PathVariable String sewadarId) {
        log.info("GET /api/attendances/sewadar/{}/summary", sewadarId);
        return ResponseEntity.ok(attendanceService.getSewadarAttendanceSummary(sewadarId));
    }

    /**
     * Get attendance summary for all sewadars with BEAS/non-BEAS breakdown
     */
    @GetMapping("/all-sewadars/summary")
    public ResponseEntity<com.rssb.application.dto.AllSewadarsAttendanceSummaryResponse> getAllSewadarsAttendanceSummary() {
        log.info("GET /api/attendances/all-sewadars/summary");
        return ResponseEntity.ok(attendanceService.getAllSewadarsAttendanceSummary());
    }
}

