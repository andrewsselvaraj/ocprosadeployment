package com.m2its.identity.auth;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, Profile user) {
    }

    public record Profile(String id, String email, String name, String orgId, List<String> roles) {
    }
}
