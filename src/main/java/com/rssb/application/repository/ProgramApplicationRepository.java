package com.rssb.application.repository;

import com.rssb.application.entity.ProgramApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramApplicationRepository extends JpaRepository<ProgramApplication, Long> {
    List<ProgramApplication> findByProgramId(Long programId);
    List<ProgramApplication> findBySewadarId(Long sewadarId);
    Optional<ProgramApplication> findByProgramIdAndSewadarId(Long programId, Long sewadarId);
    List<ProgramApplication> findByStatus(String status);
    long countByProgramId(Long programId);
    
    // Filter out DROPPED applications for incharge view
    List<ProgramApplication> findByProgramIdAndStatusNot(Long programId, String status);
    
    // Get drop requests for a program
    List<ProgramApplication> findByProgramIdAndStatus(Long programId, String status);
}

