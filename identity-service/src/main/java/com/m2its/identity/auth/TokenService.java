package com.m2its.identity.auth;

import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;

import com.m2its.common.jpa.Ids;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.security.M2itsSecurityProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/** Issues HS256 access tokens that every other service validates with the same secret. */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final M2itsSecurityProperties.Jwt settings;

    public TokenService(SecretKey jwtSecretKey, M2itsSecurityProperties properties) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(jwtSecretKey));
        this.settings = properties.jwt();
    }

    public IssuedToken issue(String email, String userId, String orgId, String name, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(settings.accessTokenTtl());
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .id(Ids.newId())
                .issuer(settings.issuer())
                .subject(email)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(AuthenticatedUser.CLAIM_USER_ID, userId)
                .claim(AuthenticatedUser.CLAIM_NAME, name)
                .claim(AuthenticatedUser.CLAIM_ROLES, roles);
        if (orgId != null) {
            claims.claim(AuthenticatedUser.CLAIM_ORG_ID, orgId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new IssuedToken(token, settings.accessTokenTtl().toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
