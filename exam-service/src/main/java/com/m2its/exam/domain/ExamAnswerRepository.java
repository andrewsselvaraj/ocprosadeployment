package com.m2its.exam.domain;

import java.util.List;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExamAnswerRepository extends JpaRepository<ExamAnswer, String> {

    List<ExamAnswer> findBySessionIdAndStatusOrderById(String sessionId, RecordStatus status);

    /**
     * One row per exam sitting: GROUP BY + aggregate functions mapped straight into a DTO
     * with "select new ...". Paging needs an explicit count query for grouped results.
     */
    @Query(value = """
            select new com.m2its.exam.domain.SessionSummary(
                     e.sessionId, e.classId, e.subjectId, e.takenBy, min(e.takenAt), count(e))
            from ExamAnswer e
            where e.orgId = :orgId and e.status = :status
              and (:takenBy is null or e.takenBy = :takenBy)
            group by e.sessionId, e.classId, e.subjectId, e.takenBy
            order by min(e.takenAt) desc""",
            countQuery = """
            select count(distinct e.sessionId) from ExamAnswer e
            where e.orgId = :orgId and e.status = :status
              and (:takenBy is null or e.takenBy = :takenBy)""")
    Page<SessionSummary> findSessions(@Param("orgId") String orgId, @Param("takenBy") String takenBy,
                                      @Param("status") RecordStatus status, Pageable pageable);
}
