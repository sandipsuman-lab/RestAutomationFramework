package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.commons.utilities.common.PlaceholderResolver;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON request template → request body. Pipeline: read template → resolve ${...} placeholders →
 * apply field overrides (JsonPath keys, {@code <remove>}, {@code <null>}) → optional POJO mapping.
 */
@Component
@RequiredArgsConstructor
public class RequestToClassObjectParser {

    private final RequestPathParser requestPathParser;
    private final PlaceholderResolver placeholderResolver;

    public String buildJson(DataType type, String fileName, Map<String, ?> overrides) {
        String template = FileUtils.readAsString(requestPathParser.getRequestTemplatePath(type, fileName));
        String resolved = placeholderResolver.resolve(template);
        Map<String, Object> resolvedOverrides = new LinkedHashMap<>();
        if (overrides != null) {
            overrides.forEach((path, value) ->
                    resolvedOverrides.put(path, value instanceof String s ? placeholderResolver.resolve(s) : value));
        }
        return JsonUtils.applyOverrides(resolved, resolvedOverrides);
    }

    public <T> T parse(DataType type, String fileName, Class<T> modelClass, Map<String, ?> overrides) {
        return JsonUtils.fromJson(buildJson(type, fileName, overrides), modelClass);
    }
}
