package com.rssb.application.repository;

import com.rssb.application.entity.Action;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActionRepository extends JpaRepository<Action, Long> {
    List<Action> findByProgramId(Long programId);
    List<Action> findByProgramIdOrderBySequenceOrderAsc(Long programId);
    List<Action> findByStatus(String status);
    List<Action> findByCreatedById(Long inchargeId);
}

