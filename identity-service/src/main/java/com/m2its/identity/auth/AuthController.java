package com.m2its.identity.auth;

import java.util.List;

import com.m2its.common.security.AuthenticatedUser;
import com.m2its.identity.auth.AuthDtos.LoginRequest;
import com.m2its.identity.auth.AuthDtos.LoginResponse;
import com.m2its.identity.auth.AuthDtos.Profile;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** Public endpoint (listed in m2its.security.public-paths). */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Echoes the caller as seen in the token: handy to check what a JWT contains. */
    @GetMapping("/me")
    public Profile me(@AuthenticationPrincipal Jwt jwt) {
        AuthenticatedUser user = AuthenticatedUser.current();
        return new Profile(user.userId(), user.email(), jwt.getClaimAsString(AuthenticatedUser.CLAIM_NAME),
                user.orgId(), List.copyOf(user.roles()));
    }
}
