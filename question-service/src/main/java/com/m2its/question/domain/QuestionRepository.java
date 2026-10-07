package com.m2its.question.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, String> {

    /** Optional chapter filter: ":chapterId is null" makes the condition a no-op when absent. */
    @Query("""
            select q from Question q
            where q.orgId = :orgId and q.subjectId = :subjectId and q.status = :status
              and (:chapterId is null or q.chapterId = :chapterId)""")
    Page<Question> search(@Param("orgId") String orgId, @Param("subjectId") String subjectId,
                          @Param("chapterId") String chapterId, @Param("status") RecordStatus status,
                          Pageable pageable);

    Optional<Question> findByIdAndStatus(String id, RecordStatus status);

    /** Loads questions plus ALL their options (also retired ones) in a single query. */
    @Query("select distinct q from Question q left join fetch q.options where q.id in :ids")
    List<Question> findWithOptionsByIdIn(@Param("ids") Collection<String> ids);
}
