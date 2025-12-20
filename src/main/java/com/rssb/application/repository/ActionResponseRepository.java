package com.rssb.application.repository;

import com.rssb.application.entity.ActionResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActionResponseRepository extends JpaRepository<ActionResponse, Long> {
    List<ActionResponse> findByActionId(Long actionId);
    List<ActionResponse> findBySewadarId(Long sewadarId);
    Optional<ActionResponse> findByActionIdAndSewadarId(Long actionId, Long sewadarId);
    List<ActionResponse> findByStatus(String status);
}

