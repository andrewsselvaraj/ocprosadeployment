package com.m2its.question.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.m2its.common.jpa.AssignedIdEntity;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.jpa.RecordStatusConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

/**
 * A question in the bank (aggregate root). Maps question_info_v_two; its answer options are
 * rows of answer_info_v_two and are saved together with it (cascade).
 */
@Entity
@Table(name = "question_info_v_two")
public class Question extends AssignedIdEntity {

    /** Legacy value of fk_chapter_id when a question has no chapter. */
    public static final String NO_CHAPTER = "None";

    @Id
    @Column(name = "pk_question_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @Column(name = "fk_subject_id", nullable = false, length = 50)
    private String subjectId;

    @Column(name = "fk_chapter_id", nullable = false, length = 50)
    private String chapterId = NO_CHAPTER;

    @Column(name = "question_name", nullable = false, length = 10000)
    private String text;

    @Column(name = "question_type", nullable = false, length = 50)
    private String type;

    @Column(name = "question_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "question_updated", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "question_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    /**
     * Bidirectional one-to-many. cascade = ALL: persisting the question persists new options.
     * Loaded lazily; for a page of questions Hibernate fetches all their options in one extra
     * query thanks to hibernate.default_batch_fetch_size (avoids N+1).
     */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @OrderBy("id")
    private List<AnswerOption> options = new ArrayList<>();

    protected Question() {
        // for JPA
    }

    public Question(String id, String orgId, String classId, String subjectId, String chapterId, String text,
                    String type, String updatedBy) {
        this.id = id;
        this.orgId = orgId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.chapterId = chapterId == null || chapterId.isBlank() ? NO_CHAPTER : chapterId;
        this.text = text;
        this.type = type;
        this.updatedBy = updatedBy;
    }

    /** Keeps both sides of the relationship in sync. */
    public AnswerOption addOption(String optionId, String optionText, boolean correct) {
        AnswerOption option = new AnswerOption(optionId, this, optionText, correct, updatedBy);
        options.add(option);
        return option;
    }

    /**
     * Options are soft-deleted rather than removed, because past exams (exam-service) still
     * reference their ids.
     */
    public void retireOptions() {
        options.forEach(o -> o.setStatus(RecordStatus.DELETED));
    }

    public List<AnswerOption> getActiveOptions() {
        return options.stream().filter(o -> o.getStatus() == RecordStatus.ACTIVE).toList();
    }

    public List<AnswerOption> getAllOptions() {
        return options;
    }

    @Override
    public String getId() {
        return id;
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

    public String getChapterId() {
        return chapterId;
    }

    public void setChapterId(String chapterId) {
        this.chapterId = chapterId == null || chapterId.isBlank() ? NO_CHAPTER : chapterId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }
}
