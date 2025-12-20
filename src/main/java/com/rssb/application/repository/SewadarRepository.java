package com.rssb.application.repository;

import com.rssb.application.entity.Sewadar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SewadarRepository extends JpaRepository<Sewadar, Long> {
}

