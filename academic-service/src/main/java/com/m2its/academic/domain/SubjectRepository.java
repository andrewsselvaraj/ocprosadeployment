package com.m2its.academic.domain;

import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, String> {

    /** Derived query on a nested property: WHERE fk_class_id = ? (no join needed). */
    List<Subject> findBySchoolClassIdAndStatusOrderByName(String classId, RecordStatus status);

    boolean existsBySchoolClassIdAndStatus(String classId, RecordStatus status);

    Optional<Subject> findByIdAndStatus(String id, RecordStatus status);
}
