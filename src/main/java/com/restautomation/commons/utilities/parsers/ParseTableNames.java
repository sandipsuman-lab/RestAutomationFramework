package com.restautomation.commons.utilities.parsers;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extracts table/view names referenced in FROM / JOIN clauses (for logging and diagnostics). */
public final class ParseTableNames {

    private static final Pattern TABLE = Pattern.compile("(?i)\\b(?:FROM|JOIN)\\s+([A-Za-z0-9_.\"$]+)");

    private ParseTableNames() {
    }

    public static Set<String> parse(String sql) {
        Set<String> tables = new LinkedHashSet<>();
        if (sql == null) {
            return tables;
        }
        Matcher matcher = TABLE.matcher(sql);
        while (matcher.find()) {
            tables.add(matcher.group(1).replace("\"", ""));
        }
        return tables;
    }
}
