package com.restautomation.commons.utilities.enumhelpers;

import com.restautomation.exceptions.ConfigurationException;
import lombok.Getter;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/** Units for date-window arithmetic. {@code symbol} is used by the ${now-4d} placeholder syntax. */
@Getter
public enum DateTimeUnit {

    SECONDS(ChronoUnit.SECONDS, "s"),
    MINUTES(ChronoUnit.MINUTES, "m"),
    HOURS(ChronoUnit.HOURS, "h"),
    DAYS(ChronoUnit.DAYS, "d"),
    WEEKS(ChronoUnit.WEEKS, "w"),
    MONTHS(ChronoUnit.MONTHS, "M"),
    YEARS(ChronoUnit.YEARS, "y");

    private final ChronoUnit chronoUnit;
    private final String symbol;

    DateTimeUnit(ChronoUnit chronoUnit, String symbol) {
        this.chronoUnit = chronoUnit;
        this.symbol = symbol;
    }

    public static DateTimeUnit fromSymbol(String symbol) {
        return Arrays.stream(values())
                .filter(unit -> unit.symbol.equals(symbol))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unknown date-time unit symbol '" + symbol + "'"));
    }

    /** Accepts "days", "DAY", "Days" etc. */
    public static DateTimeUnit fromName(String name) {
        String normalised = name.trim().toUpperCase();
        String plural = normalised.endsWith("S") ? normalised : normalised + "S";
        return Arrays.stream(values())
                .filter(unit -> unit.name().equals(plural))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unknown date-time unit '" + name + "'"));
    }
}
