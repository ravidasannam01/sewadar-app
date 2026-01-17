package com.rssb.application.repository;

import com.rssb.application.entity.Program;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.entity.SewadarFormSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SewadarFormSubmissionRepository extends JpaRepository<SewadarFormSubmission, Long> {
    List<SewadarFormSubmission> findByProgram(Program program);
    List<SewadarFormSubmission> findByProgramId(Long programId);
    Optional<SewadarFormSubmission> findByProgramAndSewadar(Program program, Sewadar sewadar);
    Optional<SewadarFormSubmission> findByProgramIdAndSewadarZonalId(Long programId, Long sewadarZonalId);
    boolean existsByProgramIdAndSewadarZonalId(Long programId, Long sewadarZonalId);
}

