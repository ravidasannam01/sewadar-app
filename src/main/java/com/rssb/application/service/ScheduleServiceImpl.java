package com.rssb.application.service;

import com.rssb.application.dto.AddressResponse;
import com.rssb.application.dto.ScheduleRequest;
import com.rssb.application.dto.ScheduleResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Schedule;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.ScheduleRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service implementation for Schedule operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final SewadarRepository sewadarRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllSchedules() {
        log.info("Fetching all schedules");
        return scheduleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleResponse getScheduleById(Long id) {
        log.info("Fetching schedule with id: {}", id);
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        return mapToResponse(schedule);
    }

    @Override
    public ScheduleResponse createSchedule(ScheduleRequest request) {
        log.info("Creating new schedule: {}", request);
        Schedule schedule = mapToEntity(request);
        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("Schedule created with id: {}", savedSchedule.getId());
        return mapToResponse(savedSchedule);
    }

    @Override
    public ScheduleResponse updateSchedule(Long id, ScheduleRequest request) {
        log.info("Updating schedule with id: {}", id);
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));

        schedule.setScheduledPlace(request.getScheduledPlace());
        schedule.setScheduledDate(request.getScheduledDate());
        schedule.setScheduledTime(request.getScheduledTime());
        schedule.setScheduledMedium(request.getScheduledMedium());

        // Update attendedBy if provided
        if (request.getAttendedById() != null) {
            Sewadar sewadar = sewadarRepository.findById(request.getAttendedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getAttendedById()));
            schedule.setAttendedBy(sewadar);
        }

        Schedule updatedSchedule = scheduleRepository.save(schedule);
        log.info("Schedule updated with id: {}", updatedSchedule.getId());
        return mapToResponse(updatedSchedule);
    }

    @Override
    public void deleteSchedule(Long id) {
        log.info("Deleting schedule with id: {}", id);
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", id));
        scheduleRepository.delete(schedule);
        log.info("Schedule deleted with id: {}", id);
    }

    private Schedule mapToEntity(ScheduleRequest request) {
        Sewadar sewadar = sewadarRepository.findById(request.getAttendedById())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getAttendedById()));

        return Schedule.builder()
                .scheduledPlace(request.getScheduledPlace())
                .scheduledDate(request.getScheduledDate())
                .scheduledTime(request.getScheduledTime())
                .scheduledMedium(request.getScheduledMedium())
                .attendedBy(sewadar)
                .build();
    }

    private ScheduleResponse mapToResponse(Schedule schedule) {
        ScheduleResponse.ScheduleResponseBuilder builder = ScheduleResponse.builder()
                .id(schedule.getId())
                .scheduledPlace(schedule.getScheduledPlace())
                .scheduledDate(schedule.getScheduledDate())
                .scheduledTime(schedule.getScheduledTime())
                .scheduledMedium(schedule.getScheduledMedium());

        if (schedule.getAttendedBy() != null) {
            Sewadar sewadar = schedule.getAttendedBy();
            SewadarResponse.SewadarResponseBuilder sewadarBuilder = SewadarResponse.builder()
                    .id(sewadar.getId())
                    .firstName(sewadar.getFirstName())
                    .lastName(sewadar.getLastName())
                    .dept(sewadar.getDept())
                    .mobile(sewadar.getMobile())
                    .remarks(sewadar.getRemarks());

            if (sewadar.getAddress() != null) {
                var address = sewadar.getAddress();
                AddressResponse addressResponse = AddressResponse.builder()
                        .id(address.getId())
                        .address1(address.getAddress1())
                        .address2(address.getAddress2())
                        .email(address.getEmail())
                        .build();
                sewadarBuilder.address(addressResponse);
            }

            builder.attendedBy(sewadarBuilder.build());
        }

        return builder.build();
    }
}

