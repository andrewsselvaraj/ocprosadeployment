package com.m2its.identity.auth;

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

/** Platform-wide administrator (SUPERADMIN). Maps the legacy admin_info table. */
@Entity
@Table(name = "admin_info")
public class PlatformAdmin extends AssignedIdEntity {

    @Id
    @Column(name = "pk_admin_id", length = 50)
    private String id;

    @Column(name = "admin_name", nullable = false, length = 50)
    private String name;

    @Column(name = "admin_email", nullable = false, length = 50)
    private String email;

    @Column(name = "admin_password", nullable = false)
    private String passwordHash;

    @Column(name = "admin_role", nullable = false, length = 50)
    private String role;

    @UpdateTimestamp
    @Column(name = "admin_updated_date_time", nullable = false)
    private LocalDateTime updatedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "admin_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected PlatformAdmin() {
        // for JPA
    }

    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public RecordStatus getStatus() {
        return status;
    }
}
