package com.rssb.application.controller;

import com.rssb.application.dto.*;
import com.rssb.application.service.DashboardService;
import com.rssb.application.util.ActionLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final ActionLogger actionLogger;

    /**
     * Get sewadars with filters, sorting, and pagination
     */
    @PostMapping("/sewadars")
    public ResponseEntity<SewadarDashboardResponse> getSewadars(@RequestBody DashboardQueryRequest request) {
        log.info("GET /api/dashboard/sewadars");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        SewadarDashboardResponse response = dashboardService.getSewadars(request, currentUserId, currentUserRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed attendance for a sewadar (one row per program-date)
     */
    @GetMapping("/sewadar/{sewadarId}/attendance")
    public ResponseEntity<SewadarDetailedAttendanceResponse> getSewadarAttendance(
            @PathVariable String sewadarId) {
        log.info("GET /api/dashboard/sewadar/{}/attendance", sewadarId);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        SewadarDetailedAttendanceResponse response = dashboardService.getSewadarDetailedAttendance(
                sewadarId, currentUserId, currentUserRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Get detailed attendance for a program (one row per sewadar with date columns)
     */
    @GetMapping("/program/{programId}/attendance")
    public ResponseEntity<ProgramDetailedAttendanceResponse> getProgramAttendance(
            @PathVariable Long programId) {
        log.info("GET /api/dashboard/program/{}/attendance", programId);
        
        ProgramDetailedAttendanceResponse response = dashboardService.getProgramDetailedAttendance(programId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get applications with filters and pagination
     */
    @PostMapping("/applications")
    public ResponseEntity<ApplicationDashboardResponse> getApplications(
            @RequestBody DashboardQueryRequest request) {
        log.info("POST /api/dashboard/applications");
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        ApplicationDashboardResponse response = dashboardService.getApplications(request, currentUserId, currentUserRole);
        return ResponseEntity.ok(response);
    }

    /**
     * Export sewadars to CSV/XLSX/PDF
     */
    @PostMapping("/sewadars/export/{format}")
    public ResponseEntity<byte[]> exportSewadars(
            @RequestBody DashboardQueryRequest request,
            @PathVariable String format) {
        log.info("POST /api/dashboard/sewadars/export/{}", format);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        long startTime = System.currentTimeMillis();
        byte[] data = dashboardService.exportSewadars(request, format.toUpperCase(), currentUserId, currentUserRole);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("format", format.toUpperCase());
        details.put("fileSizeBytes", data.length);
        details.put("durationMs", duration);
        actionLogger.logAction("EXPORT_SEWADARS", currentUserId, currentUserRole, details);
        actionLogger.logPerformance("EXPORT_SEWADARS", duration);
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = getContentType(format);
        String filename = "sewadars." + format.toLowerCase();
        
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Export sewadar detailed attendance
     */
    @GetMapping("/sewadar/{sewadarId}/attendance/export/{format}")
    public ResponseEntity<byte[]> exportSewadarAttendance(
            @PathVariable String sewadarId,
            @PathVariable String format) {
        log.info("GET /api/dashboard/sewadar/{}/attendance/export/{}", sewadarId, format);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        byte[] data = dashboardService.exportSewadarAttendance(sewadarId, format.toUpperCase(), currentUserId, currentUserRole);
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = getContentType(format);
        String filename = "sewadar_" + sewadarId + "_attendance." + format.toLowerCase();
        
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Export program detailed attendance
     */
    @GetMapping("/program/{programId}/attendance/export/{format}")
    public ResponseEntity<byte[]> exportProgramAttendance(
            @PathVariable Long programId,
            @PathVariable String format) {
        log.info("GET /api/dashboard/program/{}/attendance/export/{}", programId, format);
        
        byte[] data = dashboardService.exportProgramAttendance(programId, format.toUpperCase());
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = getContentType(format);
        String filename = "program_" + programId + "_attendance." + format.toLowerCase();
        
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    /**
     * Export applications
     */
    @PostMapping("/applications/export/{format}")
    public ResponseEntity<byte[]> exportApplications(
            @RequestBody DashboardQueryRequest request,
            @PathVariable String format) {
        log.info("POST /api/dashboard/applications/export/{}", format);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = (String) auth.getPrincipal();
        String currentUserRole = auth.getAuthorities().iterator().next().getAuthority();
        
        long startTime = System.currentTimeMillis();
        byte[] data = dashboardService.exportApplications(request, format.toUpperCase(), currentUserId, currentUserRole);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("format", format.toUpperCase());
        details.put("fileSizeBytes", data.length);
        details.put("durationMs", duration);
        actionLogger.logAction("EXPORT_APPLICATIONS", currentUserId, currentUserRole, details);
        actionLogger.logPerformance("EXPORT_APPLICATIONS", duration);
        
        HttpHeaders headers = new HttpHeaders();
        String contentType = getContentType(format);
        String filename = "applications." + format.toLowerCase();
        
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDispositionFormData("attachment", filename);
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(data);
    }

    private String getContentType(String format) {
        return switch (format.toUpperCase()) {
            case "CSV" -> "text/csv";
            case "XLSX" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "PDF" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}

