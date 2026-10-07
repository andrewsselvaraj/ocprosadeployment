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

/** A subject taught in a class. Maps subject_info; many subjects belong to one class. */
@Entity
@Table(name = "subject_info")
public class Subject extends AssignedIdEntity {

    @Id
    @Column(name = "pk_subject_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    /**
     * LAZY: the class is only loaded when accessed. Calling getSchoolClass().getId() does not
     * hit the database, because the id is already known from the fk_class_id column.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_class_id", nullable = false)
    private SchoolClass schoolClass;

    @Column(name = "subject_name", nullable = false, length = 50)
    private String name;

    @UpdateTimestamp
    @Column(name = "subject_updated", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "subject_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected Subject() {
        // for JPA
    }

    public Subject(String id, SchoolClass schoolClass, String name) {
        this.id = id;
        this.schoolClass = schoolClass;
        this.orgId = schoolClass.getOrgId();
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getOrgId() {
        return orgId;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
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
