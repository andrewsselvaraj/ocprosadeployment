package com.m2its.question.domain;

import java.time.LocalDateTime;

import com.m2its.common.jpa.AssignedIdEntity;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.jpa.RecordStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

/**
 * One answer option of a question. Maps answer_info_v_two. The org/class/subject/chapter
 * columns are denormalised copies of the question's values (kept for legacy compatibility).
 */
@Entity
@Table(name = "answer_info_v_two")
public class AnswerOption extends AssignedIdEntity {

    @Id
    @Column(name = "pk_answer_id", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_question_id", nullable = false)
    private Question question;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "fk_chapter_id", nullable = false, length = 50)
    private String chapterId;

    /**
     * mediumtext (up to 16 MB). Mapped as a plain String with columnDefinition rather than @Lob:
     * Hibernate's schema validation then accepts MySQL (type name "mediumtext") and H2 in MySQL
     * mode (which stores mediumtext as VARCHAR).
     */
    @Column(name = "answers", nullable = false, columnDefinition = "mediumtext")
    private String text;

    @Convert(converter = YesNoConverter.class)
    @Column(name = "correct_answer", nullable = false, length = 500)
    private boolean correct;

    @Column(name = "answer_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "answer_updated_date_time", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "answer_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected AnswerOption() {
        // for JPA
    }

    AnswerOption(String id, Question question, String text, boolean correct, String updatedBy) {
        this.id = id;
        this.question = question;
        this.orgId = question.getOrgId();
        this.classId = question.getClassId();
        this.subjectId = question.getSubjectId();
        this.chapterId = question.getChapterId();
        this.text = text;
        this.correct = correct;
        this.updatedBy = updatedBy;
    }

    @Override
    public String getId() {
        return id;
    }

    public Question getQuestion() {
        return question;
    }

    public String getText() {
        return text;
    }

    public boolean isCorrect() {
        return correct;
    }

    public RecordStatus getStatus() {
        return status;
    }

    void setStatus(RecordStatus status) {
        this.status = status;
    }
}
