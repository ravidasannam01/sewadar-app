package com.rssb.application.repository;

import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramNotificationPreferenceRepository extends JpaRepository<ProgramNotificationPreference, Long> {
    Optional<ProgramNotificationPreference> findByProgramAndNodeNumber(Program program, Integer nodeNumber);
    List<ProgramNotificationPreference> findByProgram(Program program);
    List<ProgramNotificationPreference> findByProgramId(Long programId);
}

