package com.rssb.application.repository;

import com.rssb.application.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByProgramId(Long programId);
    List<Attendance> findBySewadarId(Long sewadarId);
    Optional<Attendance> findByProgramIdAndSewadarId(Long programId, Long sewadarId);
    
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true")
    List<Attendance> findAttendedBySewadarId(Long sewadarId);
    
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true AND a.program.locationType = :locationType")
    List<Attendance> findAttendedBySewadarIdAndLocationType(Long sewadarId, String locationType);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true")
    Long countAttendedProgramsBySewadarId(Long sewadarId);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true AND a.program.locationType = :locationType")
    Long countAttendedProgramsBySewadarIdAndLocationType(Long sewadarId, String locationType);
    
    @Query("SELECT COALESCE(SUM(a.daysParticipated), 0) FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true")
    Integer sumDaysAttendedBySewadarId(Long sewadarId);
    
    @Query("SELECT COALESCE(SUM(a.daysParticipated), 0) FROM Attendance a WHERE a.sewadar.id = :sewadarId AND a.attended = true AND a.program.locationType = :locationType")
    Integer sumDaysAttendedBySewadarIdAndLocationType(Long sewadarId, String locationType);
}

