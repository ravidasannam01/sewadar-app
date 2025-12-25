package com.rssb.application.controller;

import com.rssb.application.dto.AttendanceRequest;
import com.rssb.application.dto.AttendanceResponse;
import com.rssb.application.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendances")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping
    public ResponseEntity<List<AttendanceResponse>> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        log.info("POST /api/attendances - Marking attendance");
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.markAttendance(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponse> updateAttendance(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean attended,
            @RequestParam(required = false) Integer daysParticipated,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, attended, daysParticipated, notes));
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
    public ResponseEntity<List<AttendanceResponse>> getAttendanceBySewadar(@PathVariable Long sewadarId) {
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
            @PathVariable Long sewadarId) {
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

