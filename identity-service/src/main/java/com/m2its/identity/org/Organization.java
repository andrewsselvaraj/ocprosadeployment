package com.m2its.identity.org;

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

/** A school / institute (tenant). Maps the legacy org_info table. */
@Entity
@Table(name = "org_info")
public class Organization extends AssignedIdEntity {

    @Id
    @Column(name = "pk_org_id", length = 50)
    private String id;

    @Column(name = "org_name", nullable = false, length = 50)
    private String name;

    @Column(name = "org_email", nullable = false, length = 50)
    private String email;

    @Column(name = "org_password", nullable = false)
    private String passwordHash;

    @Column(name = "org_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "org_updated_on", nullable = false)
    private LocalDateTime updatedOn;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "org_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected Organization() {
        // for JPA
    }

    public Organization(String id, String name, String email, String passwordHash, String updatedBy) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.updatedBy = updatedBy;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }
}
