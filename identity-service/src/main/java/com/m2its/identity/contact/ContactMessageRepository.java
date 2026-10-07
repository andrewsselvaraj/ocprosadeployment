package com.m2its.identity.contact;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, String> {

    Page<ContactMessage> findByStatus(RecordStatus status, Pageable pageable);
}
