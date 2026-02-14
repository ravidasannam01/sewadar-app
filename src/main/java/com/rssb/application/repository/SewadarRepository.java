package com.rssb.application.repository;

import com.rssb.application.entity.Role;
import com.rssb.application.entity.Sewadar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SewadarRepository extends JpaRepository<Sewadar, Long>, JpaSpecificationExecutor<Sewadar> {
    List<Sewadar> findByRole(Role role);
    List<Sewadar> findByProfession(String profession);
    Optional<Sewadar> findByMobile(String mobile);
    Optional<Sewadar> findByZonalId(String zonalId); // For login by zonal_id (String type)
}

