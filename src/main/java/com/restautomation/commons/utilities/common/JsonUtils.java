package com.restautomation.commons.utilities.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.restautomation.exceptions.FrameworkException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON helpers built on one shared, pre-configured {@link ObjectMapper} (Java time support,
 * ISO dates, unknown properties ignored) plus Jayway JsonPath read/write.
 */
public final class JsonUtils {

    /** Value used in override maps to delete a field from a JSON template. */
    public static final String REMOVE_TOKEN = "<remove>";
    /** Value used in override maps to set a field to JSON null. */
    public static final String NULL_TOKEN = "<null>";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    private static final Configuration JSON_PATH_CONFIG = Configuration.builder()
            .jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER))
            .mappingProvider(new JacksonMappingProvider(MAPPER))
            .options(Option.SUPPRESS_EXCEPTIONS)
            .build();

    private JsonUtils() {
    }

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static String toJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Unable to serialise " + value.getClass().getSimpleName() + " to JSON", e);
        }
    }

    public static String toPrettyJson(Object value) {
        try {
            Object tree = value instanceof String s ? MAPPER.readTree(s) : value;
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    public static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Invalid JSON: " + abbreviate(json), e);
        }
    }

    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Unable to map JSON to " + type.getSimpleName() + ": " + e.getOriginalMessage(), e);
        }
    }

    public static <T> T fromJson(String json, TypeReference<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new FrameworkException("Unable to map JSON to " + type.getType() + ": " + e.getOriginalMessage(), e);
        }
    }

    public static <T> T convert(Object source, Class<T> type) {
        return MAPPER.convertValue(source, type);
    }

    public static <T> List<T> convertList(Object source, Class<T> elementType) {
        return MAPPER.convertValue(source, MAPPER.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    public static Map<String, Object> toMap(Object source) {
        return MAPPER.convertValue(source, new TypeReference<LinkedHashMap<String, Object>>() {
        });
    }

    public static boolean isValidJson(String json) {
        try {
            MAPPER.readTree(json);
            return json != null && !json.isBlank();
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- JsonPath

    /** Reads a JsonPath expression; returns {@code null} when the path does not exist. */
    public static JsonNode read(String json, String jsonPath) {
        return read(readTree(json), jsonPath);
    }

    public static JsonNode read(JsonNode root, String jsonPath) {
        JsonNode result = JsonPath.using(JSON_PATH_CONFIG).parse(root).read(normalisePath(jsonPath));
        return result == null || result.isMissingNode() ? null : result;
    }

    public static <T> T read(String json, String jsonPath, Class<T> type) {
        JsonNode node = read(json, jsonPath);
        return node == null ? null : MAPPER.convertValue(node, type);
    }

    public static boolean pathExists(JsonNode root, String jsonPath) {
        try {
            JsonPath.using(Configuration.builder().jsonProvider(new JacksonJsonNodeJsonProvider(MAPPER)).build())
                    .parse(root).read(normalisePath(jsonPath));
            return true;
        } catch (PathNotFoundException e) {
            return false;
        }
    }

    /**
     * Applies overrides to a JSON document. Keys are JsonPath expressions or plain field names
     * ({@code price} == {@code $.price}). Values {@value #REMOVE_TOKEN} and {@value #NULL_TOKEN}
     * delete the field or set it to null; strings are converted with {@link #toTypedValue(String)}.
     */
    public static String applyOverrides(String json, Map<String, ?> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return json;
        }
        DocumentContext doc = JsonPath.using(JSON_PATH_CONFIG).parse(readTree(json));
        overrides.forEach((rawPath, rawValue) -> {
            String path = normalisePath(rawPath);
            if (REMOVE_TOKEN.equals(rawValue)) {
                doc.delete(path);
                return;
            }
            Object value = NULL_TOKEN.equals(rawValue) ? null
                    : rawValue instanceof String s ? toTypedValue(s) : rawValue;
            JsonNode valueNode = MAPPER.valueToTree(value);
            if (pathExists(doc.json(), path)) {
                doc.set(path, valueNode);
            } else {
                int split = path.lastIndexOf('.');
                doc.put(path.substring(0, split), path.substring(split + 1), valueNode);
            }
        });
        return toJson(doc.json());
    }

    /**
     * Interprets a string from a feature file as a JSON literal when it looks like one
     * (number, boolean, object, array); quote it ({@code "123"}) to force a string.
     */
    public static Object toTypedValue(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        if (value.matches("-?\\d+(\\.\\d+)?") || value.startsWith("{") || value.startsWith("[")) {
            try {
                return MAPPER.readTree(value);
            } catch (JsonProcessingException ignored) {
                return raw;
            }
        }
        return raw;
    }

    /** Removes the named fields (at any depth) from a copy of the node. */
    public static JsonNode withoutFields(JsonNode node, Set<String> fieldNames) {
        JsonNode copy = node.deepCopy();
        stripFields(copy, fieldNames);
        return copy;
    }

    private static void stripFields(JsonNode node, Set<String> fieldNames) {
        if (node instanceof ObjectNode object) {
            object.remove(fieldNames);
            object.forEach(child -> stripFields(child, fieldNames));
        } else if (node instanceof ArrayNode array) {
            array.forEach(child -> stripFields(child, fieldNames));
        }
    }

    /**
     * Flattens nested objects into dot-notation keys ({@code price.amount}). Arrays of scalars are
     * kept as lists; arrays of objects are indexed ({@code lines[0].sku}).
     */
    public static Map<String, Object> flatten(JsonNode node) {
        Map<String, Object> result = new LinkedHashMap<>();
        flatten("", node, result);
        return result;
    }

    private static void flatten(String prefix, JsonNode node, Map<String, Object> out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                flatten(prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey(), field.getValue(), out);
            }
        } else if (node.isArray()) {
            boolean allScalars = true;
            for (JsonNode element : node) {
                allScalars &= element.isValueNode();
            }
            if (allScalars) {
                List<Object> values = new ArrayList<>();
                node.forEach(element -> values.add(scalarValue(element)));
                out.put(prefix, values);
            } else {
                for (int i = 0; i < node.size(); i++) {
                    flatten(prefix + "[" + i + "]", node.get(i), out);
                }
            }
        } else {
            out.put(prefix, scalarValue(node));
        }
    }

    public static Object scalarValue(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isNumber()) {
            return node.decimalValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }

    private static String normalisePath(String path) {
        String trimmed = path.trim();
        return trimmed.startsWith("$") ? trimmed : "$." + trimmed;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }
}
