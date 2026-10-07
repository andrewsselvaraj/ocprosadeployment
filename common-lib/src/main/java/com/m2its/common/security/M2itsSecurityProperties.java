package com.m2its.common.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Security settings shared by every service.
 *
 * <pre>
 * m2its:
 *   security:
 *     public-paths: [/api/auth/login]
 *     jwt:
 *       secret: ${JWT_SECRET}      # >= 32 bytes, injected from a Kubernetes Secret
 *       issuer: m2its-identity
 *       access-token-ttl: PT1H
 * </pre>
 */
@ConfigurationProperties("m2its.security")
public record M2itsSecurityProperties(@DefaultValue Jwt jwt, @DefaultValue List<String> publicPaths) {

    public record Jwt(String secret,
                      @DefaultValue("m2its-identity") String issuer,
                      @DefaultValue("PT1H") Duration accessTokenTtl) {
    }
}
