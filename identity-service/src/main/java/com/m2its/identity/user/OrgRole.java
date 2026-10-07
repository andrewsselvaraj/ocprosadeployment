package com.m2its.identity.user;

import java.util.Arrays;

import com.m2its.common.security.Roles;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Role of an organisation member. Stored as the legacy strings 'Admin' / 'Data Entry Operator' / 'User'. */
public enum OrgRole {
    ADMIN("Admin", Roles.ADMIN),
    DATA_ENTRY("Data Entry Operator", Roles.DATA_ENTRY),
    USER("User", Roles.USER);

    private final String dbValue;
    private final String tokenRole;

    OrgRole(String dbValue, String tokenRole) {
        this.dbValue = dbValue;
        this.tokenRole = tokenRole;
    }

    public String tokenRole() {
        return tokenRole;
    }

    @Converter
    public static class DbConverter implements AttributeConverter<OrgRole, String> {

        @Override
        public String convertToDatabaseColumn(OrgRole role) {
            return role == null ? null : role.dbValue;
        }

        @Override
        public OrgRole convertToEntityAttribute(String value) {
            return value == null ? null : Arrays.stream(values())
                    .filter(r -> r.dbValue.equalsIgnoreCase(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user_role: " + value));
        }
    }
}
