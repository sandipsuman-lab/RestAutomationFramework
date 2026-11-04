package com.restautomation.commons.utilities.readerhelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.exceptions.ConfigurationException;
import com.restautomation.exceptions.FixtureNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Single entry point for tabular test data in external files. Picks the reader by extension:
 * <ul>
 *   <li>{@code .csv}  – header row + records</li>
 *   <li>{@code .xlsx/.xls} – {@code selector} = sheet name (first sheet when empty)</li>
 *   <li>{@code .json/.yml/.yaml} – array at the root, or at JsonPath {@code selector}</li>
 * </ul>
 * Relative paths are tried as-is first, then under the configured test-data root.
 */
@Component
@RequiredArgsConstructor
public class TestDataReader {

    private final ReadConfigYaml config;

    public List<Map<String, Object>> readRecords(String path, String selector) {
        String resolved = resolvePath(path);
        return switch (FileUtils.extension(resolved)) {
            case "csv" -> CsvFileReader.readAll(resolved);
            case "xlsx", "xls" -> ExcelFileReader.readSheet(resolved, selector);
            case "json" -> toRecords(JsonUtils.readTree(FileUtils.readAsString(resolved)), selector, resolved);
            case "yml", "yaml" -> toRecords(YamlResourceReader.parse(FileUtils.readAsString(resolved)), selector, resolved);
            default -> throw new ConfigurationException("Unsupported test-data file type: " + resolved);
        };
    }

    public List<Map<String, Object>> readRecords(String path) {
        return readRecords(path, null);
    }

    public <T> List<T> readRecordsAs(String path, String selector, Class<T> type) {
        return JsonUtils.convertList(readRecords(path, selector), type);
    }

    public String resolvePath(String path) {
        if (FileUtils.exists(path)) {
            return path;
        }
        String underRoot = FileUtils.join(config.getPaths().getTestData(), path);
        if (FileUtils.exists(underRoot)) {
            return underRoot;
        }
        throw new FixtureNotFoundException("Test-data file not found: " + path + " (also tried " + underRoot + ")");
    }

    private static List<Map<String, Object>> toRecords(JsonNode root, String jsonPath, String source) {
        JsonNode node = jsonPath == null || jsonPath.isBlank() ? root : JsonUtils.read(root, jsonPath);
        if (node == null || !node.isArray()) {
            throw new ConfigurationException("Expected an array in " + source + (jsonPath == null ? "" : " at " + jsonPath));
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        node.forEach(element -> rows.add(JsonUtils.flatten(element)));
        return rows;
    }
}
