package com.m2its.identity.contact;

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

/** "Contact us" message from the public website. Maps the legacy contact_m2i table. */
@Entity
@Table(name = "contact_m2i")
public class ContactMessage extends AssignedIdEntity {

    @Id
    @Column(name = "m_contact_id", length = 50)
    private String id;

    @Column(name = "m_name", nullable = false, length = 50)
    private String name;

    @Column(name = "m_email", nullable = false, length = 50)
    private String email;

    @Column(name = "m_subject", nullable = false, length = 50)
    private String subject;

    @Column(name = "m_message", nullable = false, length = 500)
    private String message;

    @CreationTimestamp
    @Column(name = "m_message_updated_date_time", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Convert(converter = RecordStatusConverter.class)
    @Column(name = "m_message_status", nullable = false, length = 50)
    private RecordStatus status = RecordStatus.ACTIVE;

    protected ContactMessage() {
        // for JPA
    }

    public ContactMessage(String id, String name, String email, String subject, String message) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.subject = subject;
        this.message = message;
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

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public RecordStatus getStatus() {
        return status;
    }
}
