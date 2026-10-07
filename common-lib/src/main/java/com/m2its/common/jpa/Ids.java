package com.m2its.common.jpa;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Primary keys are application-assigned strings (legacy rows use 13-char PHP uniqid values
 * in varchar(50) columns). New ids are 32 hex chars: a 14-char time prefix (microsecond
 * clock, strictly increasing within this JVM) followed by 18 random chars.
 *
 * Time-ordered ids (same idea as UUIDv7) keep MySQL B-tree inserts sequential and let
 * "ORDER BY id" return rows in creation order, e.g. the answer options of a question.
 */
public final class Ids {

    private static final AtomicLong LAST = new AtomicLong();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static String newId() {
        long nowMicros = System.currentTimeMillis() * 1000;
        long stamp = LAST.updateAndGet(prev -> Math.max(prev + 1, nowMicros));
        byte[] random = new byte[9];
        RANDOM.nextBytes(random);
        return String.format("%014x", stamp) + HexFormat.of().formatHex(random);
    }
}
