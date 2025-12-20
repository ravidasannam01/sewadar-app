package com.rssb.application.service;

import com.rssb.application.dto.ScheduleRequest;
import com.rssb.application.dto.ScheduleResponse;

import java.util.List;

/**
 * Service interface for Schedule operations.
 */
public interface ScheduleService {
    List<ScheduleResponse> getAllSchedules();
    ScheduleResponse getScheduleById(Long id);
    ScheduleResponse createSchedule(ScheduleRequest request);
    ScheduleResponse updateSchedule(Long id, ScheduleRequest request);
    void deleteSchedule(Long id);
}

