package com.m2its.identity.auth;

import java.util.List;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAdminRepository extends JpaRepository<PlatformAdmin, String> {

    List<PlatformAdmin> findByEmailAndStatus(String email, RecordStatus status);
}
