package com.rssb.application.repository;

import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgramWorkflowRepository extends JpaRepository<ProgramWorkflow, Long> {
    Optional<ProgramWorkflow> findByProgram(Program program);
    Optional<ProgramWorkflow> findByProgramId(Long programId);
}

