package com.m2its.academic.domain;

import java.time.LocalDateTime;

import com.m2its.common.jpa.AssignedIdEntity;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.jpa.RecordStatusConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.UpdateTimestamp;

/** A class / grade within an organisation, e.g. "Class 10". Maps class_info. */
@Entity
@Table(name = "class_info")
public class SchoolClass extends AssignedIdEntity {

    @Id
    @Column(name = "pk_class_id", length = 50)
    private String id;

    @Column(name = "fk_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "class_name", nullable = false, length = 50)
    private String name;

    @UpdateTimestamp
    @Column(name = "class_updated", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "class_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected SchoolClass() {
        // for JPA
    }

    public SchoolClass(String id, String orgId, String name) {
        this.id = id;
        this.orgId = orgId;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getOrgId() {
        return orgId;
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
