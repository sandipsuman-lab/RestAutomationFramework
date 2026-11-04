package com.restautomation.commons.utilities.datavalidation;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controls how {@link DataComparator} matches rows and compares values.
 */
@Getter
@Builder(toBuilder = true)
public class ComparisonOptions {

    /** Columns that identify a row in both data sets. Empty = order-independent full-row matching. */
    @Singular
    private final List<String> keyColumns;

    /** Renames applied to the actual data set before comparison: actual column → expected column. */
    @Singular("mapColumn")
    private final Map<String, String> columnMapping;

    @Singular
    private final Set<String> ignoreColumns;

    /** When non-empty, only these columns are compared. Otherwise the columns common to both sides. */
    @Singular
    private final Set<String> includeColumns;

    /**
     * Match column names ignoring case and the separators {@code _ - space}, so
     * {@code itemId}, {@code item_id} and {@code Item Id} are the same column.
     */
    @Builder.Default
    private final boolean looseColumnNames = true;

    @Builder.Default
    private final BigDecimal numericTolerance = BigDecimal.ZERO;

    @Builder.Default
    private final boolean caseSensitive = true;

    @Builder.Default
    private final boolean trimWhitespace = true;

    @Builder.Default
    private final boolean nullEqualsEmpty = true;

    @Builder.Default
    private final boolean compareDatesAsInstants = true;

    @Builder.Default
    private final boolean ignoreRowOrder = true;

    @Builder.Default
    private final boolean checkRowCount = true;

    /** Fail when a column of the expected data set is absent from the actual one. */
    @Builder.Default
    private final boolean failOnMissingColumns = false;

    /** Mismatches printed in the assertion message (all are kept in the report file). */
    @Builder.Default
    private final int maxMismatchesInMessage = 25;

    public static ComparisonOptions defaults() {
        return ComparisonOptions.builder().build();
    }

    public static ComparisonOptions byKeys(String... keys) {
        return ComparisonOptions.builder().keyColumns(List.of(keys)).build();
    }
}
