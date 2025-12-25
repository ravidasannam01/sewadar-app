package com.rssb.application.repository;

import com.rssb.application.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByProgramId(Long programId);
    
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId")
    List<Attendance> findBySewadarZonalId(@Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT a FROM Attendance a WHERE a.program.id = :programId AND a.sewadar.zonalId = :sewadarZonalId")
    Optional<Attendance> findByProgramIdAndSewadarZonalId(@Param("programId") Long programId, @Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true")
    List<Attendance> findAttendedBySewadarId(@Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    List<Attendance> findAttendedBySewadarIdAndLocationType(@Param("sewadarZonalId") Long sewadarZonalId, @Param("locationType") String locationType);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true")
    Long countAttendedProgramsBySewadarId(@Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    Long countAttendedProgramsBySewadarIdAndLocationType(@Param("sewadarZonalId") Long sewadarZonalId, @Param("locationType") String locationType);
    
    @Query("SELECT COALESCE(SUM(a.daysParticipated), 0) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true")
    Integer sumDaysAttendedBySewadarId(@Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT COALESCE(SUM(a.daysParticipated), 0) FROM Attendance a WHERE a.sewadar.zonalId = :sewadarZonalId AND a.attended = true AND (CASE WHEN UPPER(a.program.location) = 'BEAS' THEN 'BEAS' ELSE 'NON_BEAS' END) = :locationType")
    Integer sumDaysAttendedBySewadarIdAndLocationType(@Param("sewadarZonalId") Long sewadarZonalId, @Param("locationType") String locationType);
    
    // Legacy methods for backward compatibility (deprecated)
    @Deprecated
    @Query("SELECT a FROM Attendance a WHERE a.sewadar.zonalId = :sewadarId")
    default List<Attendance> findBySewadarId(Long sewadarId) {
        return findBySewadarZonalId(sewadarId);
    }
    
    @Deprecated
    @Query("SELECT a FROM Attendance a WHERE a.program.id = :programId AND a.sewadar.zonalId = :sewadarId")
    default Optional<Attendance> findByProgramIdAndSewadarId(Long programId, Long sewadarId) {
        return findByProgramIdAndSewadarZonalId(programId, sewadarId);
    }
}

