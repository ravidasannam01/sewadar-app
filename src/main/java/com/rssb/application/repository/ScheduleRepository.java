package com.rssb.application.repository;

import com.rssb.application.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByAttendedById(Long attendedById);
    List<Schedule> findByScheduledDate(LocalDate scheduledDate);
}

