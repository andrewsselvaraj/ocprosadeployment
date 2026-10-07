package com.m2its.identity.user;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private UserDtos() {
    }

    /** orgId is only used (and required) when a SUPERADMIN creates the user. */
    public record CreateUserRequest(
            String orgId,
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotNull OrgRole role) {
    }

    /** password is optional: null keeps the current one. */
    public record UpdateUserRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email @Size(max = 50) String email,
            @NotNull OrgRole role,
            @Size(min = 8, max = 72) String password) {
    }

    public record UserResponse(String id, String orgId, String name, String email, OrgRole role,
                               String status, String updatedBy, LocalDateTime updatedAt) {

        static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getOrgId(), user.getName(), user.getEmail(), user.getRole(),
                    user.getStatus().name(), user.getUpdatedBy(), user.getUpdatedAt());
        }
    }
}
