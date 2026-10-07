package com.m2its.academic.domain;

import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChapterRepository extends JpaRepository<Chapter, String> {

    List<Chapter> findBySubjectIdAndStatusOrderByName(String subjectId, RecordStatus status);

    Optional<Chapter> findByIdAndStatus(String id, RecordStatus status);

    /**
     * JPQL with JOIN FETCH: loads chapters and their subjects in ONE query, avoiding the
     * N+1 problem when building the class tree.
     */
    @Query("""
            select c from Chapter c join fetch c.subject s
            where c.classId = :classId and c.status = :status and s.status = :status
            order by s.name, c.name""")
    List<Chapter> findTreeByClass(@Param("classId") String classId, @Param("status") RecordStatus status);
}
