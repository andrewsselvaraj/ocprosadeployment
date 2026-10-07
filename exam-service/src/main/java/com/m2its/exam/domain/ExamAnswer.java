package com.m2its.exam.domain;

import java.time.LocalDateTime;

import com.m2its.common.jpa.AssignedIdEntity;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.jpa.RecordStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/** One answered question of an exam sitting. Maps exam_info. Immutable once written. */
@Entity
@Table(name = "exam_info")
public class ExamAnswer extends AssignedIdEntity {

    @Id
    @Column(name = "pk_exam_id", length = 50)
    private String id;

    @Column(name = "exam_session_id", nullable = false, length = 50, updatable = false)
    private String sessionId;

    @Column(name = "fk_org_id", nullable = false, length = 50, updatable = false)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50, updatable = false)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50, updatable = false)
    private String subjectId;

    @Column(name = "fk_question_id", nullable = false, length = 50, updatable = false)
    private String questionId;

    @Column(name = "fk_user_selected_answer_id", nullable = false, length = 50, updatable = false)
    private String selectedAnswerId;

    @Column(name = "exam_taken_by", nullable = false, length = 50, updatable = false)
    private String takenBy;

    @CreationTimestamp
    @Column(name = "exam_taken_date_time", nullable = false, updatable = false)
    private LocalDateTime takenAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "exam_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected ExamAnswer() {
        // for JPA
    }

    public ExamAnswer(String id, String sessionId, String orgId, String classId, String subjectId, String questionId,
                      String selectedAnswerId, String takenBy) {
        this.id = id;
        this.sessionId = sessionId;
        this.orgId = orgId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.questionId = questionId;
        this.selectedAnswerId = selectedAnswerId;
        this.takenBy = takenBy;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getClassId() {
        return classId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getSelectedAnswerId() {
        return selectedAnswerId;
    }

    public String getTakenBy() {
        return takenBy;
    }

    public LocalDateTime getTakenAt() {
        return takenAt;
    }

    public RecordStatus getStatus() {
        return status;
    }
}
