package com.m2its.academic.domain;

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
 * A chapter of a subject. Maps chapter_info. The legacy table also stores fk_org_id and
 * fk_class_id (denormalised copies); they are filled from the subject on creation.
 */
@Entity
@Table(name = "chapter_info")
public class Chapter extends AssignedIdEntity {

    @Id
    @Column(name = "pk_chapter_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "fk_class_id", nullable = false, length = 50)
    private String classId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_subject_id", nullable = false)
    private Subject subject;

    @Column(name = "chapter_name", nullable = false, length = 50)
    private String name;

    @UpdateTimestamp
    @Column(name = "chapter_updated", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "chapter_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected Chapter() {
        // for JPA
    }

    public Chapter(String id, Subject subject, String name) {
        this.id = id;
        this.subject = subject;
        this.orgId = subject.getOrgId();
        this.classId = subject.getSchoolClass().getId();
        this.name = name;
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

    public Subject getSubject() {
        return subject;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
