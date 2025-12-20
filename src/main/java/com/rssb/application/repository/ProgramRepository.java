package com.rssb.application.repository;

import com.rssb.application.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByStatus(String status);
    
    @Query("SELECT DISTINCT p FROM Program p JOIN p.programDates pd WHERE pd.programDate = :date")
    List<Program> findByProgramDate(@Param("date") LocalDate date);
    
    @Query("SELECT DISTINCT p FROM Program p JOIN p.programDates pd WHERE pd.programDate BETWEEN :start AND :end")
    List<Program> findByProgramDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
    
    List<Program> findByCreatedById(Long inchargeId);
}

