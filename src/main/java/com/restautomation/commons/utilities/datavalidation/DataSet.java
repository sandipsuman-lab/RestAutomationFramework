package com.restautomation.commons.utilities.datavalidation;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.exceptions.DataValidationException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source-independent tabular data: a name (for reporting) and rows of column → value.
 * Nested JSON objects are flattened to dot-notation columns.
 */
@Getter
public class DataSet {

    private final String name;
    private final List<Map<String, Object>> rows;

    public DataSet(String name, List<Map<String, Object>> rows) {
        this.name = name;
        this.rows = rows == null ? List.of() : rows;
    }

    public static DataSet fromRows(String name, List<Map<String, Object>> rows) {
        return new DataSet(name, rows);
    }

    /** JSON array (or a single object, treated as one row). */
    public static DataSet fromJson(String name, JsonNode node) {
        if (node == null || node.isNull()) {
            throw new DataValidationException("No JSON data for data set '" + name + "'");
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(element -> rows.add(JsonUtils.flatten(element)));
        } else {
            rows.add(JsonUtils.flatten(node));
        }
        return new DataSet(name, rows);
    }

    /** Array at {@code jsonPath} of a JSON document. */
    public static DataSet fromJson(String name, String json, String jsonPath) {
        JsonNode node = jsonPath == null || jsonPath.isBlank() ? JsonUtils.readTree(json) : JsonUtils.read(json, jsonPath);
        if (node == null) {
            throw new DataValidationException("JsonPath '" + jsonPath + "' not found for data set '" + name + "'");
        }
        return fromJson(name, node);
    }

    /** List of POJOs (API, DB or legacy models). */
    public static DataSet fromObjects(String name, Collection<?> objects) {
        return fromJson(name, JsonUtils.mapper().valueToTree(objects));
    }

    public int size() {
        return rows.size();
    }

    public Set<String> columns() {
        Set<String> columns = new LinkedHashSet<>();
        rows.forEach(row -> columns.addAll(row.keySet()));
        return columns;
    }

    /** Copy with every column renamed by {@code renamer}. */
    public DataSet renameColumns(java.util.function.UnaryOperator<String> renamer) {
        List<Map<String, Object>> renamed = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> copy = new LinkedHashMap<>();
            row.forEach((column, value) -> copy.put(renamer.apply(column), value));
            renamed.add(copy);
        }
        return new DataSet(name, renamed);
    }
}
