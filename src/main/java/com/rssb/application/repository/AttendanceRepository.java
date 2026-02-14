package com.rssb.application.repository;

import com.rssb.application.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    // Find all attendance records for a program
    List<Attendance> findByProgramId(Long programId);
    
    // Find all attendance records for a sewadar
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId")
    List<Attendance> findBySewadarZonalId(@Param("sewadarZonalId") String sewadarZonalId);
    
    // Find attendance for a specific sewadar-program-date combination
    @Query("SELECT a FROM Attendance a WHERE a.programDate.id = :programDateId AND a.sewadar.zonalId = :sewadarZonalId")
    Optional<Attendance> findByProgramDateIdAndSewadarZonalId(
            @Param("programDateId") Long programDateId, 
            @Param("sewadarZonalId") String sewadarZonalId);
    
    // Find all attendance records for a sewadar in a program (all dates)
    @Query("SELECT a FROM Attendance a WHERE a.program.id = :programId AND a.sewadar.zonalId = :sewadarZonalId")
    List<Attendance> findByProgramIdAndSewadarZonalId(@Param("programId") Long programId, @Param("sewadarZonalId") String sewadarZonalId);
    
    // Find attendance for a specific program date
    @Query("SELECT a FROM Attendance a WHERE a.programDate.id = :programDateId")
    List<Attendance> findByProgramDateId(@Param("programDateId") Long programDateId);
    
    // Count distinct programs a sewadar attended (using DISTINCT program_id)
    @Query("SELECT COUNT(DISTINCT a.program.id) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId")
    Long countAttendedProgramsBySewadarId(@Param("sewadarZonalId") String sewadarZonalId);
    
    // Count distinct programs by location type
    @Query("SELECT COUNT(DISTINCT a.program.id) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    Long countAttendedProgramsBySewadarIdAndLocationType(@Param("sewadarZonalId") String sewadarZonalId, @Param("locationType") String locationType);
    
    // Count total days attended (count of attendance records)
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId")
    Long countDaysAttendedBySewadarId(@Param("sewadarZonalId") String sewadarZonalId);
    
    // Count days by location type
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    Long countDaysAttendedBySewadarIdAndLocationType(@Param("sewadarZonalId") String sewadarZonalId, @Param("locationType") String locationType);
    
    // Get all attendance records for a sewadar (for summary)
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId")
    List<Attendance> findAttendedBySewadarId(@Param("sewadarZonalId") String sewadarZonalId);
    
    // Get attendance records by location type
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    List<Attendance> findAttendedBySewadarIdAndLocationType(@Param("sewadarZonalId") String sewadarZonalId, @Param("locationType") String locationType);
}

