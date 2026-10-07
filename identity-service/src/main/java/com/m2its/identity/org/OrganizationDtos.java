package com.m2its.identity.org;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response shapes for /api/orgs. Passwords are write-only and never returned. */
public final class OrganizationDtos {

    private OrganizationDtos() {
    }

    public record CreateOrganizationRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email,
            @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record UpdateOrganizationRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email) {
    }

    public record OrganizationResponse(String id, String name, String email, String status,
                                       String updatedBy, LocalDateTime updatedOn) {

        static OrganizationResponse from(Organization org) {
            return new OrganizationResponse(org.getId(), org.getName(), org.getEmail(), org.getStatus().name(),
                    org.getUpdatedBy(), org.getUpdatedOn());
        }
    }
}
