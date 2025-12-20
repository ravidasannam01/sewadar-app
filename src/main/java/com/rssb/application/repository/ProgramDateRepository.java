package com.rssb.application.repository;

import com.rssb.application.entity.ProgramDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgramDateRepository extends JpaRepository<ProgramDate, Long> {
    List<ProgramDate> findByProgramId(Long programId);
    List<ProgramDate> findByProgramIdOrderByProgramDateAsc(Long programId);
}

