package com.restautomation.commons.utilities.common;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Random test-data generators for unique identifiers, names and values. */
public final class RandomDataUtils {

    private static final String ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomDataUtils() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    public static String alphabetic(int length) {
        return pick(ALPHA, length);
    }

    public static String alphanumeric(int length) {
        return pick(ALPHA + DIGITS, length);
    }

    /** Numeric string without a leading zero. */
    public static String numeric(int length) {
        if (length <= 0) {
            return "";
        }
        return pick("123456789", 1) + pick(DIGITS, length - 1);
    }

    public static int intBetween(int minInclusive, int maxInclusive) {
        return minInclusive + RANDOM.nextInt(maxInclusive - minInclusive + 1);
    }

    public static String email() {
        return "qa." + alphanumeric(8).toLowerCase() + "@example.com";
    }

    public static <T> T oneOf(List<T> values) {
        return values.get(RANDOM.nextInt(values.size()));
    }

    public static Instant instantBetween(Instant start, Instant end) {
        long span = end.toEpochMilli() - start.toEpochMilli();
        return Instant.ofEpochMilli(start.toEpochMilli() + (long) (RANDOM.nextDouble() * span));
    }

    private static String pick(String source, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(source.charAt(RANDOM.nextInt(source.length())));
        }
        return builder.toString();
    }
}
