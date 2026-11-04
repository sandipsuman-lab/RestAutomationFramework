package com.restautomation.commons.utilities.readerhelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.exceptions.FixtureNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reads expected-response fixtures ({@code schemas/<Api>Schema/<file>.json}) and any other JSON file.
 * Missing files or keys fail immediately with {@link FixtureNotFoundException}.
 */
@Component
@RequiredArgsConstructor
public class ReadJsonFile {

    private final ReadConfigYaml config;
    private final Map<String, JsonNode> cache = new ConcurrentHashMap<>();

    /**
     * @param folder fixture folder under the schemas root, e.g. {@code GetItemsSchema}
     * @param file   file name, e.g. {@code getItemsSchema.json}
     * @param key    top-level key naming one expected payload
     */
    public JsonNode getJsonData(String folder, String file, String key) {
        String path = FileUtils.join(config.getPaths().getSchemas(), folder, file);
        JsonNode root = cache.computeIfAbsent(path, p -> JsonUtils.readTree(FileUtils.readAsString(p)));
        JsonNode node = root.get(key);
        if (node == null) {
            throw new FixtureNotFoundException("Key '" + key + "' not found in fixture " + path
                    + ". Available keys: " + iterableToList(root.fieldNames()));
        }
        return node.deepCopy();
    }

    public <T> T getJsonData(String folder, String file, String key, Class<T> type) {
        return JsonUtils.convert(getJsonData(folder, file, key), type);
    }

    public JsonNode readJson(String path) {
        return JsonUtils.readTree(FileUtils.readAsString(path));
    }

    public <T> T readJsonAs(String path, Class<T> type) {
        return JsonUtils.fromJson(FileUtils.readAsString(path), type);
    }

    public <T> List<T> readJsonList(String path, Class<T> elementType) {
        return JsonUtils.convertList(readJson(path), elementType);
    }

    private static List<String> iterableToList(java.util.Iterator<String> iterator) {
        List<String> list = new java.util.ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }
}
