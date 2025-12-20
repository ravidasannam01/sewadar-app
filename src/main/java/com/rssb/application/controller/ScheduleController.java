package com.rssb.application.controller;

import com.rssb.application.dto.ScheduleRequest;
import com.rssb.application.dto.ScheduleResponse;
import com.rssb.application.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Schedule operations.
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * Get all schedules.
     *
     * @return List of all schedules
     */
    @GetMapping
    public ResponseEntity<List<ScheduleResponse>> getAllSchedules() {
        log.info("GET /api/schedules - Fetching all schedules");
        List<ScheduleResponse> schedules = scheduleService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * Get a schedule by ID.
     *
     * @param id The schedule ID
     * @return The schedule response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getScheduleById(@PathVariable Long id) {
        log.info("GET /api/schedules/{} - Fetching schedule by id", id);
        ScheduleResponse schedule = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(schedule);
    }

    /**
     * Create a new schedule.
     *
     * @param request The schedule request DTO
     * @return The created schedule response
     */
    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        log.info("POST /api/schedules - Creating new schedule");
        ScheduleResponse createdSchedule = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSchedule);
    }

    /**
     * Update an existing schedule.
     *
     * @param id      The schedule ID
     * @param request The schedule request DTO
     * @return The updated schedule response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRequest request) {
        log.info("PUT /api/schedules/{} - Updating schedule", id);
        ScheduleResponse updatedSchedule = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(updatedSchedule);
    }

    /**
     * Delete a schedule by ID.
     *
     * @param id The schedule ID
     * @return No content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        log.info("DELETE /api/schedules/{} - Deleting schedule", id);
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}

