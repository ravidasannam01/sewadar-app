package com.rssb.application.repository;

import com.rssb.application.entity.ProgramSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramSelectionRepository extends JpaRepository<ProgramSelection, Long> {
    List<ProgramSelection> findByProgramId(Long programId);
    List<ProgramSelection> findBySewadarId(Long sewadarId);
    Optional<ProgramSelection> findByProgramIdAndSewadarId(Long programId, Long sewadarId);
    List<ProgramSelection> findByStatus(String status);
    
    @Query("SELECT ps FROM ProgramSelection ps WHERE ps.program.id = :programId AND ps.status != 'DROPPED' ORDER BY ps.priorityScore DESC NULLS LAST")
    List<ProgramSelection> findByProgramIdOrderByPriority(Long programId);
    
    @Query("SELECT ps FROM ProgramSelection ps WHERE ps.program.id = :programId ORDER BY ps.priorityScore DESC NULLS LAST")
    List<ProgramSelection> findByProgramIdOrderByPriorityIncludingDropped(Long programId);
    long countByProgramId(Long programId);
    
    @Query("SELECT COUNT(ps) FROM ProgramSelection ps WHERE ps.program.id = :programId AND ps.status != :status")
    long countByProgramIdAndStatusNot(Long programId, String status);
}

