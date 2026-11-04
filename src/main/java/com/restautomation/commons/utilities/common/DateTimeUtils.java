package com.restautomation.commons.utilities.common;

import com.restautomation.commons.utilities.enumhelpers.DateTimeOperation;
import com.restautomation.commons.utilities.enumhelpers.DateTimeUnit;
import com.restautomation.exceptions.FrameworkException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;

/** UTC-centric date/time helpers: window arithmetic, multi-format parsing and formatting. */
public final class DateTimeUtils {

    public static final ZoneOffset UTC = ZoneOffset.UTC;
    public static final String ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /** "yyyy-MM-dd HH:mm:ss" with optional fraction, as returned by most JDBC drivers. */
    private static final DateTimeFormatter SQL_DATE_TIME = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd()
            .toFormatter();

    private static final List<DateTimeFormatter> OFFSET_FORMATS = List.of(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            DateTimeFormatter.ISO_ZONED_DATE_TIME);

    private static final List<DateTimeFormatter> LOCAL_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            SQL_DATE_TIME);

    private DateTimeUtils() {
    }

    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    public static ZonedDateTime shift(ZonedDateTime base, DateTimeOperation operation, long amount, DateTimeUnit unit) {
        return operation.apply(base, amount, unit);
    }

    public static ZonedDateTime fromNow(DateTimeOperation operation, long amount, DateTimeUnit unit) {
        return shift(nowUtc(), operation, amount, unit);
    }

    public static String format(TemporalAccessor value, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        if (value instanceof Instant instant) {
            return formatter.withZone(UTC).format(instant);
        }
        if (value instanceof ZonedDateTime zoned) {
            return formatter.format(zoned.withZoneSameInstant(UTC));
        }
        return formatter.format(value);
    }

    public static String toIsoUtc(TemporalAccessor value) {
        return format(value, ISO_UTC_PATTERN);
    }

    /**
     * Parses ISO instants/offset date-times, local date-times (assumed UTC), SQL timestamps,
     * plain dates (start of day UTC) and epoch milliseconds.
     */
    public static Instant parseToInstant(String value) {
        return tryParse(value).orElseThrow(() -> new FrameworkException("Unrecognised date/time value: '" + value + "'"));
    }

    public static ZonedDateTime parseToUtc(String value) {
        return parseToInstant(value).atZone(UTC);
    }

    public static Optional<Instant> tryParse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String text = value.trim();
        try {
            return Optional.of(Instant.parse(text));
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        for (DateTimeFormatter formatter : OFFSET_FORMATS) {
            try {
                return Optional.of(OffsetDateTime.parse(text, formatter).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        for (DateTimeFormatter formatter : LOCAL_FORMATS) {
            try {
                return Optional.of(LocalDateTime.parse(text, formatter).toInstant(UTC));
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        try {
            return Optional.of(LocalDate.parse(text).atStartOfDay(UTC).toInstant());
        } catch (DateTimeParseException ignored) {
            // try next format
        }
        if (text.matches("\\d{12,13}")) {
            return Optional.of(Instant.ofEpochMilli(Long.parseLong(text)));
        }
        return Optional.empty();
    }

    /** True only for values that contain a date part, so plain numbers are not treated as dates. */
    public static boolean isParseable(String value) {
        return value != null && value.matches("^\\d{4}-\\d{2}-\\d{2}.*") && tryParse(value).isPresent();
    }

    public static boolean isWithin(Instant value, Instant start, Instant end) {
        return !value.isBefore(start) && !value.isAfter(end);
    }

    public static boolean sameInstant(String first, String second) {
        Optional<Instant> a = tryParse(first);
        Optional<Instant> b = tryParse(second);
        return a.isPresent() && a.equals(b);
    }

    public static ZonedDateTime startOfDay(ZonedDateTime value) {
        return value.truncatedTo(ChronoUnit.DAYS);
    }

    public static ZonedDateTime endOfDay(ZonedDateTime value) {
        return startOfDay(value).plusDays(1).minusSeconds(1);
    }
}
