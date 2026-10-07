package com.m2its.academic.domain;

import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, String> {

    List<SchoolClass> findByOrgIdAndStatusOrderByName(String orgId, RecordStatus status);

    Optional<SchoolClass> findByIdAndStatus(String id, RecordStatus status);
}
