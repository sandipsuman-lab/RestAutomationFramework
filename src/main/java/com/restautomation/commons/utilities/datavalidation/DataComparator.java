package com.restautomation.commons.utilities.datavalidation;

import com.restautomation.commons.utilities.parsers.DataConversionHelper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Compares an actual data set (usually the API payload) with an expected one (warehouse rows,
 * CSV / Excel / JSON file, legacy service) and collects every difference instead of stopping at
 * the first one.
 * <ol>
 *   <li>Column names are normalised (see {@link ComparisonOptions#isLooseColumnNames()}) and mapped.</li>
 *   <li>Rows are paired by key columns, or by sorted content / position when no keys are given.</li>
 *   <li>Values are compared with {@link DataConversionHelper#valuesEqual}.</li>
 * </ol>
 */
@Component
public class DataComparator {

    public ComparisonReport compare(DataSet actual, DataSet expected, ComparisonOptions options) {
        UnaryOperator<String> normaliser = options.isLooseColumnNames() ? DataComparator::looseName : UnaryOperator.identity();
        Map<String, String> mapping = new LinkedHashMap<>();
        options.getColumnMapping().forEach((from, to) -> mapping.put(normaliser.apply(from), normaliser.apply(to)));

        DataSet normalisedActual = actual.renameColumns(c -> {
            String name = normaliser.apply(c);
            return mapping.getOrDefault(name, name);
        });
        DataSet normalisedExpected = expected.renameColumns(normaliser);
        List<String> keys = options.getKeyColumns().stream().map(normaliser).toList();
        Set<String> ignore = options.getIgnoreColumns().stream().map(normaliser).collect(Collectors.toSet());

        Set<String> actualColumns = normalisedActual.columns();
        Set<String> expectedColumns = normalisedExpected.columns();
        Set<String> columns = new LinkedHashSet<>();
        if (!options.getIncludeColumns().isEmpty()) {
            options.getIncludeColumns().stream().map(normaliser).forEach(columns::add);
        } else {
            expectedColumns.stream().filter(actualColumns::contains).forEach(columns::add);
        }
        columns.removeAll(ignore);

        ComparisonReport report = new ComparisonReport(actual, expected, columns);
        Set<String> missingColumns = expectedColumns.stream()
                .filter(c -> !actualColumns.contains(c) && !ignore.contains(c))
                .filter(c -> options.getIncludeColumns().isEmpty() || columns.contains(c))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!missingColumns.isEmpty()) {
            if (options.isFailOnMissingColumns() || !options.getIncludeColumns().isEmpty()) {
                missingColumns.forEach(c -> report.add(new Mismatch(Mismatch.Type.MISSING_COLUMN, null, c, null, null)));
            } else {
                report.note("Expected-only columns not compared: " + missingColumns);
            }
        }
        Set<String> actualOnly = actualColumns.stream().filter(c -> !expectedColumns.contains(c) && !ignore.contains(c))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (!actualOnly.isEmpty()) {
            report.note("Actual-only columns not compared: " + actualOnly);
        }
        if (columns.isEmpty()) {
            report.add(new Mismatch(Mismatch.Type.MISSING_COLUMN, null, "<none in common>", expectedColumns, actualColumns));
            return report;
        }

        if (options.isCheckRowCount() && actual.size() != expected.size()) {
            report.add(new Mismatch(Mismatch.Type.ROW_COUNT, null, null, expected.size(), actual.size()));
        }

        if (!keys.isEmpty()) {
            compareByKey(normalisedActual, normalisedExpected, keys, columns, options, report);
        } else {
            compareByPosition(normalisedActual, normalisedExpected, columns, options, report);
        }
        return report;
    }

    private void compareByKey(DataSet actual, DataSet expected, List<String> keys, Set<String> columns,
                              ComparisonOptions options, ComparisonReport report) {
        Map<String, Map<String, Object>> actualByKey = index(actual, keys, "actual", report);
        Map<String, Map<String, Object>> expectedByKey = index(expected, keys, "expected", report);
        expectedByKey.forEach((key, expectedRow) -> {
            Map<String, Object> actualRow = actualByKey.get(key);
            if (actualRow == null) {
                report.add(new Mismatch(Mismatch.Type.MISSING_ROW, key, null, null, null));
            } else {
                compareRow(key, expectedRow, actualRow, columns, options, report);
            }
        });
        actualByKey.keySet().stream()
                .filter(key -> !expectedByKey.containsKey(key))
                .forEach(key -> report.add(new Mismatch(Mismatch.Type.EXTRA_ROW, key, null, null, null)));
    }

    private void compareByPosition(DataSet actual, DataSet expected, Set<String> columns,
                                   ComparisonOptions options, ComparisonReport report) {
        List<Map<String, Object>> actualRows = new ArrayList<>(actual.getRows());
        List<Map<String, Object>> expectedRows = new ArrayList<>(expected.getRows());
        if (options.isIgnoreRowOrder()) {
            Comparator<Map<String, Object>> byContent = Comparator.comparing(row -> rowSignature(row, columns));
            actualRows.sort(byContent);
            expectedRows.sort(byContent);
        }
        int common = Math.min(actualRows.size(), expectedRows.size());
        for (int i = 0; i < common; i++) {
            compareRow("#" + (i + 1), expectedRows.get(i), actualRows.get(i), columns, options, report);
        }
        for (int i = common; i < expectedRows.size(); i++) {
            report.add(new Mismatch(Mismatch.Type.MISSING_ROW, rowSignature(expectedRows.get(i), columns), null, null, null));
        }
        for (int i = common; i < actualRows.size(); i++) {
            report.add(new Mismatch(Mismatch.Type.EXTRA_ROW, rowSignature(actualRows.get(i), columns), null, null, null));
        }
    }

    private void compareRow(String key, Map<String, Object> expectedRow, Map<String, Object> actualRow,
                            Set<String> columns, ComparisonOptions options, ComparisonReport report) {
        boolean rowMatches = true;
        for (String column : columns) {
            if (!expectedRow.containsKey(column) && !actualRow.containsKey(column)) {
                continue;
            }
            Object expectedValue = expectedRow.get(column);
            Object actualValue = actualRow.get(column);
            if (!DataConversionHelper.valuesEqual(expectedValue, actualValue, options)) {
                report.add(new Mismatch(Mismatch.Type.VALUE, key, column, expectedValue, actualValue));
                rowMatches = false;
            }
        }
        if (rowMatches) {
            report.rowMatched();
        }
    }

    private static Map<String, Map<String, Object>> index(DataSet data, List<String> keys, String side, ComparisonReport report) {
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        for (Map<String, Object> row : data.getRows()) {
            String key = keys.stream()
                    .map(k -> k + "=" + DataConversionHelper.canonical(row.get(k)))
                    .collect(Collectors.joining(", ", "[", "]"));
            if (indexed.putIfAbsent(key, row) != null) {
                report.add(new Mismatch(Mismatch.Type.DUPLICATE_KEY, key, side, null, null));
            }
        }
        return indexed;
    }

    private static String rowSignature(Map<String, Object> row, Set<String> columns) {
        return columns.stream()
                .map(c -> DataConversionHelper.canonical(row.get(c)))
                .collect(Collectors.joining("|", "[", "]"));
    }

    /** {@code Item_Id}, {@code itemId}, {@code item id} → {@code itemid}. Dots of nested paths are kept. */
    static String looseName(String column) {
        return column == null ? null : column.replaceAll("[_\\-\\s]", "").toLowerCase();
    }
}
