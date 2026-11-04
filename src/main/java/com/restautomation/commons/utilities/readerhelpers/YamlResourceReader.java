package com.restautomation.commons.utilities.readerhelpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.exceptions.ConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Reads YAML files into typed objects. Spring placeholders such as {@code ${api.gateway.base-url}}
 * are resolved against the active profile before parsing, so one YAML file serves every environment.
 */
@Component
@RequiredArgsConstructor
public class YamlResourceReader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final Environment environment;

    public <T> T read(String path, Class<T> type) {
        String resolved = environment.resolvePlaceholders(FileUtils.readAsString(path));
        try {
            return YAML.readValue(resolved, type);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse YAML file '" + path + "' as " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    public JsonNode readTree(String path) {
        String resolved = environment.resolvePlaceholders(FileUtils.readAsString(path));
        try {
            return YAML.readTree(resolved);
        } catch (Exception e) {
            throw new ConfigurationException("Unable to parse YAML file '" + path + "': " + e.getMessage(), e);
        }
    }

    /** Plain YAML parsing without placeholder resolution (for test-data files). */
    public static JsonNode parse(String yamlText) {
        try {
            return YAML.readTree(yamlText);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid YAML: " + e.getMessage(), e);
        }
    }
}
