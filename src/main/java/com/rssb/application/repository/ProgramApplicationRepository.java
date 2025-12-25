package com.rssb.application.repository;

import com.rssb.application.entity.ProgramApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramApplicationRepository extends JpaRepository<ProgramApplication, Long> {
    List<ProgramApplication> findByProgramId(Long programId);
    
    @Query("SELECT pa FROM ProgramApplication pa WHERE pa.sewadar.zonalId = :sewadarZonalId")
    List<ProgramApplication> findBySewadarZonalId(@Param("sewadarZonalId") Long sewadarZonalId);
    
    @Query("SELECT pa FROM ProgramApplication pa WHERE pa.program.id = :programId AND pa.sewadar.zonalId = :sewadarZonalId")
    Optional<ProgramApplication> findByProgramIdAndSewadarZonalId(@Param("programId") Long programId, @Param("sewadarZonalId") Long sewadarZonalId);
    
    List<ProgramApplication> findByStatus(String status);
    long countByProgramId(Long programId);
    
    // Filter out DROPPED applications for incharge view
    List<ProgramApplication> findByProgramIdAndStatusNot(Long programId, String status);
    
    // Get drop requests for a program
    List<ProgramApplication> findByProgramIdAndStatus(Long programId, String status);
    
    // Legacy methods for backward compatibility (deprecated)
    @Deprecated
    @Query("SELECT pa FROM ProgramApplication pa WHERE pa.sewadar.zonalId = :sewadarId")
    default List<ProgramApplication> findBySewadarId(Long sewadarId) {
        return findBySewadarZonalId(sewadarId);
    }
    
    @Deprecated
    @Query("SELECT pa FROM ProgramApplication pa WHERE pa.program.id = :programId AND pa.sewadar.zonalId = :sewadarId")
    default Optional<ProgramApplication> findByProgramIdAndSewadarId(Long programId, Long sewadarId) {
        return findByProgramIdAndSewadarZonalId(programId, sewadarId);
    }
}

