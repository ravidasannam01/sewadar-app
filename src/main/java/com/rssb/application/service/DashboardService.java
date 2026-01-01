package com.rssb.application.service;

import com.rssb.application.dto.*;
import org.springframework.data.domain.Pageable;

public interface DashboardService {
    /**
     * Get sewadars with filters, sorting, and pagination
     */
    SewadarDashboardResponse getSewadars(DashboardQueryRequest request, Long currentUserId, String currentUserRole);
    
    /**
     * Get detailed attendance for a sewadar (one row per program-date)
     */
    SewadarDetailedAttendanceResponse getSewadarDetailedAttendance(Long sewadarId, Long currentUserId, String currentUserRole);
    
    /**
     * Get detailed attendance for a program (one row per sewadar with date columns)
     */
    ProgramDetailedAttendanceResponse getProgramDetailedAttendance(Long programId);
    
    /**
     * Get applications with filters and pagination
     */
    ApplicationDashboardResponse getApplications(DashboardQueryRequest request, Long currentUserId, String currentUserRole);
    
    /**
     * Export sewadars to CSV/XLSX/PDF
     */
    byte[] exportSewadars(DashboardQueryRequest request, String format, Long currentUserId, String currentUserRole);
    
    /**
     * Export sewadar detailed attendance to CSV/XLSX/PDF
     */
    byte[] exportSewadarAttendance(Long sewadarId, String format, Long currentUserId, String currentUserRole);
    
    /**
     * Export program detailed attendance to CSV/XLSX/PDF
     */
    byte[] exportProgramAttendance(Long programId, String format);
    
    /**
     * Export applications to CSV/XLSX/PDF
     */
    byte[] exportApplications(DashboardQueryRequest request, String format, Long currentUserId, String currentUserRole);
}

