package com.m2its.identity.user;

import java.util.List;
import java.util.Optional;

import com.m2its.common.jpa.RecordStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, String> {

    /** Legacy data has duplicate emails, so login checks every active match, newest first. */
    List<AppUser> findByEmailAndStatusOrderByUpdatedAtDesc(String email, RecordStatus status);

    boolean existsByEmailAndStatus(String email, RecordStatus status);

    Page<AppUser> findByOrgIdAndStatus(String orgId, RecordStatus status, Pageable pageable);

    Optional<AppUser> findByIdAndStatus(String id, RecordStatus status);
}
