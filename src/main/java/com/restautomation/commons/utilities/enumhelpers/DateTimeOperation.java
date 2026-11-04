package com.restautomation.commons.utilities.enumhelpers;

import java.time.ZonedDateTime;

/** Direction of date-window arithmetic. */
public enum DateTimeOperation {

    PLUS {
        @Override
        public ZonedDateTime apply(ZonedDateTime base, long amount, DateTimeUnit unit) {
            return base.plus(amount, unit.getChronoUnit());
        }
    },
    MINUS {
        @Override
        public ZonedDateTime apply(ZonedDateTime base, long amount, DateTimeUnit unit) {
            return base.minus(amount, unit.getChronoUnit());
        }
    };

    public abstract ZonedDateTime apply(ZonedDateTime base, long amount, DateTimeUnit unit);

    /** Accepts "plus", "minus", "+", "-". */
    public static DateTimeOperation fromName(String name) {
        String value = name.trim().toLowerCase();
        return value.equals("plus") || value.equals("+") ? PLUS : MINUS;
    }
}
