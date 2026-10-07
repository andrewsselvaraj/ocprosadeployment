package com.m2its.identity.user;

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

/** A member of an organisation (admin or student). Maps the legacy user_info table. */
@Entity
@Table(name = "user_info")
public class AppUser extends AssignedIdEntity {

    @Id
    @Column(name = "pk_user_id", length = 50)
    private String id;

    @Column(name = "user_org_id", nullable = false, length = 50)
    private String orgId;

    @Column(name = "user_name", nullable = false, length = 50)
    private String name;

    @Column(name = "user_email", nullable = false, length = 50)
    private String email;

    @Column(name = "user_password", nullable = false)
    private String passwordHash;

    @Convert(converter = OrgRole.DbConverter.class)
    @Column(name = "user_role", nullable = false, length = 50)
    private OrgRole role;

    @Column(name = "user_updated_by", nullable = false, length = 50)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "user_updated_date_time", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "user_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected AppUser() {
        // for JPA
    }

    public AppUser(String id, String orgId, String name, String email, String passwordHash, OrgRole role,
                   String updatedBy) {
        this.id = id;
        this.orgId = orgId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.updatedBy = updatedBy;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public OrgRole getRole() {
        return role;
    }

    public void setRole(OrgRole role) {
        this.role = role;
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
