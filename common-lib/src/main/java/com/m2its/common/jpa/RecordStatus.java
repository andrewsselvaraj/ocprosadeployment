package com.m2its.common.jpa;

import java.util.Arrays;

/**
 * Legacy "*_status" column values. Rows are never physically deleted: DELETE endpoints
 * set the status to {@link #DELETED} so exam history keeps pointing at valid rows.
 */
public enum RecordStatus {
    ACTIVE("A"),
    INACTIVE("I"),
    DELETED("D"),
    /** Older spelling of "deleted" found in legacy question/answer rows; kept so rows round-trip unchanged. */
    DELETED_LEGACY("DEL"),
    /** Seen in the legacy dump; meaning undocumented, treated as not active. */
    CLOSED("C");

    private final String code;

    RecordStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static RecordStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(s -> s.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown status code: " + code));
    }
}
