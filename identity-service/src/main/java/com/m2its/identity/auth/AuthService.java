package com.m2its.identity.auth;

import java.util.List;

import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.Roles;
import com.m2its.common.web.ApiException;
import com.m2its.identity.auth.AuthDtos.LoginRequest;
import com.m2its.identity.auth.AuthDtos.LoginResponse;
import com.m2its.identity.auth.AuthDtos.Profile;
import com.m2its.identity.auth.TokenService.IssuedToken;
import com.m2its.identity.user.AppUser;
import com.m2its.identity.user.AppUserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository users;
    private final PlatformAdminRepository admins;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    /** Compared against when no account matches, so response time does not reveal unknown emails. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PlatformAdminRepository admins, PasswordEncoder passwordEncoder,
                       TokenService tokens) {
        this.users = users;
        this.admins = admins;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * Checks platform admins first, then organisation users. Legacy {noop} passwords are
     * re-hashed with BCrypt on success; the change is saved by JPA dirty checking on commit.
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();

        for (PlatformAdmin admin : admins.findByEmailAndStatus(email, RecordStatus.ACTIVE)) {
            if (passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
                if (passwordEncoder.upgradeEncoding(admin.getPasswordHash())) {
                    admin.setPasswordHash(passwordEncoder.encode(request.password()));
                }
                return respond(new Profile(admin.getId(), admin.getEmail(), admin.getName(), null,
                        List.of(Roles.SUPERADMIN)));
            }
        }

        List<AppUser> candidates = users.findByEmailAndStatusOrderByUpdatedAtDesc(email, RecordStatus.ACTIVE);
        for (AppUser user : candidates) {
            if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
                    user.setPasswordHash(passwordEncoder.encode(request.password()));
                    log.info("Upgraded legacy password hash for user {}", user.getId());
                }
                return respond(new Profile(user.getId(), user.getEmail(), user.getName(), user.getOrgId(),
                        List.of(user.getRole().tokenRole())));
            }
        }

        if (candidates.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyHash);
        }
        log.info("Failed login attempt for {}", email);
        throw ApiException.unauthorized("Invalid email or password");
    }

    private LoginResponse respond(Profile profile) {
        IssuedToken token = tokens.issue(profile.email(), profile.id(), profile.orgId(), profile.name(), profile.roles());
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), profile);
    }
}
