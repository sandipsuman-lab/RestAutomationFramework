package com.restautomation.unittests;

import com.restautomation.commons.utilities.datavalidation.ComparisonOptions;
import com.restautomation.commons.utilities.datavalidation.ComparisonReport;
import com.restautomation.commons.utilities.datavalidation.DataComparator;
import com.restautomation.commons.utilities.datavalidation.DataSet;
import com.restautomation.commons.utilities.datavalidation.Mismatch;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataComparatorTest {

    private final DataComparator comparator = new DataComparator();

    @Test
    public void matchesAcrossNamingStylesTypesAndDateFormats() {
        DataSet api = DataSet.fromRows("api", List.of(
                row("itemId", "ITM-1", "price", new BigDecimal("25.50"), "updatedAt", "2026-10-12T08:00:00Z", "active", true)));
        DataSet db = DataSet.fromRows("db", List.of(
                row("ITEM_ID", "ITM-1", "PRICE", "25.5", "UPDATED_AT", "2026-10-12 08:00:00", "ACTIVE", "Y")));

        ComparisonReport report = comparator.compare(api, db, ComparisonOptions.byKeys("itemId"));

        assertThat(report.isMatch()).as(report.summary()).isTrue();
        assertThat(report.getMatchedRows()).isEqualTo(1);
    }

    @Test
    public void reportsValueMissingAndExtraRows() {
        DataSet actual = DataSet.fromRows("actual", List.of(
                row("id", "1", "name", "A"),
                row("id", "3", "name", "C")));
        DataSet expected = DataSet.fromRows("expected", List.of(
                row("id", "1", "name", "Changed"),
                row("id", "2", "name", "B")));

        ComparisonReport report = comparator.compare(actual, expected, ComparisonOptions.byKeys("id"));

        assertThat(report.getMismatches()).extracting(Mismatch::type)
                .containsExactlyInAnyOrder(Mismatch.Type.VALUE, Mismatch.Type.MISSING_ROW, Mismatch.Type.EXTRA_ROW);
    }

    @Test
    public void appliesToleranceIgnoredColumnsAndMapping() {
        DataSet actual = DataSet.fromRows("actual", List.of(row("sku", "X", "amount", "10.004", "ts", "now")));
        DataSet expected = DataSet.fromRows("expected", List.of(row("productCode", "X", "amount", "10.00", "ts", "then")));

        ComparisonOptions options = ComparisonOptions.builder()
                .keyColumn("productCode")
                .mapColumn("sku", "productCode")
                .ignoreColumn("ts")
                .numericTolerance(new BigDecimal("0.01"))
                .build();

        assertThat(comparator.compare(actual, expected, options).isMatch()).isTrue();
    }

    @Test
    public void comparesWithoutKeysIgnoringOrder() {
        DataSet actual = DataSet.fromRows("actual", List.of(row("v", "b"), row("v", "a")));
        DataSet expected = DataSet.fromRows("expected", List.of(row("v", "a"), row("v", "b")));

        assertThat(comparator.compare(actual, expected, ComparisonOptions.defaults()).isMatch()).isTrue();
    }

    @Test
    public void flagsRowCountAndDuplicateKeys() {
        DataSet actual = DataSet.fromRows("actual", List.of(row("id", "1"), row("id", "1")));
        DataSet expected = DataSet.fromRows("expected", List.of(row("id", "1")));

        ComparisonReport report = comparator.compare(actual, expected, ComparisonOptions.byKeys("id"));

        assertThat(report.getMismatches()).extracting(Mismatch::type)
                .contains(Mismatch.Type.ROW_COUNT, Mismatch.Type.DUPLICATE_KEY);
        assertThat(report.toCsv()).startsWith("type,rowKey,column,expected,actual");
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }
}
