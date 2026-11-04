package com.restautomation.commons.utilities.datavalidation;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Result of comparing two {@link DataSet}s. */
@Getter
public class ComparisonReport {

    private final String actualName;
    private final String expectedName;
    private final int actualRowCount;
    private final int expectedRowCount;
    private final Set<String> comparedColumns;
    private final List<Mismatch> mismatches = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private int matchedRows;

    public ComparisonReport(DataSet actual, DataSet expected, Set<String> comparedColumns) {
        this.actualName = actual.getName();
        this.expectedName = expected.getName();
        this.actualRowCount = actual.size();
        this.expectedRowCount = expected.size();
        this.comparedColumns = comparedColumns;
    }

    void add(Mismatch mismatch) {
        mismatches.add(mismatch);
    }

    void note(String note) {
        notes.add(note);
    }

    void rowMatched() {
        matchedRows++;
    }

    public boolean isMatch() {
        return mismatches.isEmpty();
    }

    public List<Mismatch> getMismatches() {
        return Collections.unmodifiableList(mismatches);
    }

    public String summary(int maxMismatches) {
        StringBuilder text = new StringBuilder()
                .append("Data comparison: actual='").append(actualName).append("' (").append(actualRowCount).append(" rows)")
                .append(" vs expected='").append(expectedName).append("' (").append(expectedRowCount).append(" rows)\n")
                .append("Compared columns: ").append(comparedColumns).append('\n')
                .append("Fully matching rows: ").append(matchedRows).append(", mismatches: ").append(mismatches.size()).append('\n');
        notes.forEach(note -> text.append("Note: ").append(note).append('\n'));
        mismatches.stream().limit(maxMismatches).forEach(m -> text.append("  - ").append(m.describe()).append('\n'));
        if (mismatches.size() > maxMismatches) {
            text.append("  ... ").append(mismatches.size() - maxMismatches).append(" more (see report file)\n");
        }
        return text.toString();
    }

    public String summary() {
        return summary(Integer.MAX_VALUE);
    }

    public String toCsv() {
        StringBuilder csv = new StringBuilder("type,rowKey,column,expected,actual\n");
        mismatches.forEach(m -> csv.append(m.type()).append(',')
                .append(escape(m.rowKey())).append(',')
                .append(escape(m.column())).append(',')
                .append(escape(m.expected())).append(',')
                .append(escape(m.actual())).append('\n'));
        return csv.toString();
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        return text.contains(",") || text.contains("\"") || text.contains("\n")
                ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }
}
