package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.common.DateTimeUtils;
import com.restautomation.commons.utilities.datavalidation.ComparisonOptions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Type conversion and normalised equality used when comparing values from different sources
 * (API JSON, warehouse rows, CSV/Excel cells, legacy XML), where the same value often arrives with
 * different types or formats: "10" vs 10 vs 10.00, "Y" vs true, ISO vs SQL timestamps.
 */
public final class DataConversionHelper {

    private static final Set<String> TRUE_VALUES = Set.of("true", "y", "yes", "1", "t");
    private static final Set<String> FALSE_VALUES = Set.of("false", "n", "no", "0", "f");

    private DataConversionHelper() {
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        String text = value.toString().trim().replace(",", "");
        return text.matches("[-+]?\\d*\\.?\\d+([eE][-+]?\\d+)?") ? new BigDecimal(text) : null;
    }

    public static Integer toInteger(Object value) {
        BigDecimal decimal = toBigDecimal(value);
        return decimal == null ? null : decimal.intValueExact();
    }

    public static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().trim().toLowerCase();
        if (TRUE_VALUES.contains(text)) {
            return true;
        }
        if (FALSE_VALUES.contains(text)) {
            return false;
        }
        return null;
    }

    public static Optional<Instant> toInstant(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Instant instant) {
            return Optional.of(instant);
        }
        String text = value.toString();
        return DateTimeUtils.isParseable(text) ? DateTimeUtils.tryParse(text) : Optional.empty();
    }

    public static boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    /** Equality with default comparison options. */
    public static boolean valuesEqual(Object expected, Object actual) {
        return valuesEqual(expected, actual, ComparisonOptions.defaults());
    }

    /**
     * Normalised equality: null/empty, numeric (with tolerance), boolean, date-time (same instant)
     * and string (trim / case) comparison, in that order.
     */
    public static boolean valuesEqual(Object expected, Object actual, ComparisonOptions options) {
        expected = joinIfCollection(expected);
        actual = joinIfCollection(actual);
        if (isBlank(expected) || isBlank(actual)) {
            if (options.isNullEqualsEmpty()) {
                return isBlank(expected) && isBlank(actual);
            }
            return expected == null ? actual == null : expected.equals(actual);
        }
        BigDecimal expectedNumber = toBigDecimal(expected);
        BigDecimal actualNumber = toBigDecimal(actual);
        if (expectedNumber != null && actualNumber != null) {
            return expectedNumber.subtract(actualNumber).abs().compareTo(options.getNumericTolerance()) <= 0;
        }
        if (expected instanceof Boolean || actual instanceof Boolean) {
            Boolean a = toBoolean(expected);
            Boolean b = toBoolean(actual);
            if (a != null && b != null) {
                return a.equals(b);
            }
        }
        if (options.isCompareDatesAsInstants()) {
            Optional<Instant> expectedInstant = toInstant(expected);
            Optional<Instant> actualInstant = toInstant(actual);
            if (expectedInstant.isPresent() && actualInstant.isPresent()) {
                return expectedInstant.get().equals(actualInstant.get());
            }
        }
        String a = expected.toString();
        String b = actual.toString();
        if (options.isTrimWhitespace()) {
            a = a.trim();
            b = b.trim();
        }
        return options.isCaseSensitive() ? a.equals(b) : a.equalsIgnoreCase(b);
    }

    /**
     * Lists compare as their comma-joined form, so ["a","b"] equals "a,b" (services often flatten
     * arrays to delimited strings on read-back).
     */
    private static Object joinIfCollection(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).map(String::trim).collect(Collectors.joining(","));
        }
        return value;
    }

    /** Canonical string used for sorting and composite keys. */
    public static String canonical(Object value) {
        value = joinIfCollection(value);
        if (isBlank(value)) {
            return "";
        }
        BigDecimal number = toBigDecimal(value);
        if (number != null) {
            return number.stripTrailingZeros().toPlainString();
        }
        Optional<Instant> instant = toInstant(value);
        if (instant.isPresent()) {
            return instant.get().toString();
        }
        return value.toString().trim();
    }
}
