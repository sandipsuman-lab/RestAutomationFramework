package com.restautomation.commons.utilities.datavalidation;

/**
 * One difference found by {@link DataComparator}.
 */
public record Mismatch(Type type, String rowKey, String column, Object expected, Object actual) {

    public enum Type {
        ROW_COUNT,
        MISSING_ROW,
        EXTRA_ROW,
        DUPLICATE_KEY,
        MISSING_COLUMN,
        VALUE
    }

    public String describe() {
        return switch (type) {
            case ROW_COUNT -> "Row count: expected " + expected + " but was " + actual;
            case MISSING_ROW -> "Missing row " + rowKey + " (present in expected data only)";
            case EXTRA_ROW -> "Unexpected row " + rowKey + " (present in actual data only)";
            case DUPLICATE_KEY -> "Duplicate key " + rowKey + " in " + column + " data";
            case MISSING_COLUMN -> "Column '" + column + "' missing from actual data";
            case VALUE -> "Row " + rowKey + ", column '" + column + "': expected [" + expected + "] but was [" + actual + "]";
        };
    }
}
