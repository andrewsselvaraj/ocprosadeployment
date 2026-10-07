package com.m2its.identity.org;

import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, String> {

    List<Organization> findByStatusOrderByName(RecordStatus status);

    Optional<Organization> findByIdAndStatus(String id, RecordStatus status);
}
