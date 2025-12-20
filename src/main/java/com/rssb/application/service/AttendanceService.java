package com.rssb.application.service;

import com.rssb.application.dto.AttendanceRequest;
import com.rssb.application.dto.AttendanceResponse;

import java.util.List;

public interface AttendanceService {
    List<AttendanceResponse> markAttendance(AttendanceRequest request);
    AttendanceResponse updateAttendance(Long id, Boolean attended, Integer daysParticipated, String notes);
    List<AttendanceResponse> getAttendanceByProgram(Long programId);
    List<AttendanceResponse> getAttendanceBySewadar(Long sewadarId);
    List<AttendanceResponse> getAttendanceStatistics(Long programId);
    com.rssb.application.dto.SewadarAttendanceSummaryResponse getSewadarAttendanceSummary(Long sewadarId);
    com.rssb.application.dto.AllSewadarsAttendanceSummaryResponse getAllSewadarsAttendanceSummary();
}

