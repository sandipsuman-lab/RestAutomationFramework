package com.restautomation.unittests;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonComparator;
import com.restautomation.commons.utilities.common.JsonUtils;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilitiesTest {

    @Test
    public void appliesOverridesRemovalsAndNewFields() {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("price", "10.5");
        overrides.put("$.name", JsonUtils.REMOVE_TOKEN);
        overrides.put("nested.flag", "true");
        overrides.put("code", "\"007\"");

        JsonNode result = JsonUtils.readTree(JsonUtils.applyOverrides(
                "{\"name\":\"a\",\"price\":1,\"nested\":{},\"code\":\"x\"}", overrides));

        assertThat(result.has("name")).isFalse();
        assertThat(result.get("price").decimalValue()).isEqualByComparingTo("10.5");
        assertThat(result.get("nested").get("flag").booleanValue()).isTrue();
        assertThat(result.get("code").asText()).isEqualTo("007");
    }

    @Test
    public void flattensNestedObjectsAndArrays() {
        Map<String, Object> flat = JsonUtils.flatten(JsonUtils.readTree(
                "{\"a\":{\"b\":1},\"tags\":[\"x\",\"y\"],\"lines\":[{\"sku\":\"s1\"}]}"));

        assertThat(flat).containsKeys("a.b", "tags", "lines[0].sku");
    }

    @Test
    public void comparatorSupportsMatcherTokensAndIgnoredFields() {
        JsonNode expected = JsonUtils.readTree("""
                {"id":"<notEmpty>","status":"<oneOf:received|complete>","at":"<isoDateTime>",
                 "n":"<number>","href":"<regex:.*/ops/1$>","traceId":"ignored","list":[{"v":1},{"v":2}]}""");
        JsonNode actual = JsonUtils.readTree("""
                {"id":"op-1","status":"complete","at":"2026-10-28T10:00:00Z","n":5,
                 "href":"http://h/ops/1","traceId":"abc","list":[{"v":2},{"v":1}],"extra":true}""");

        assertThat(JsonComparator.compare(expected, actual, Set.of("traceId"))).isEmpty();
    }

    @Test
    public void comparatorReportsEveryDifference() {
        JsonNode expected = JsonUtils.readTree("{\"a\":1,\"b\":\"x\",\"c\":\"<notNull>\"}");
        JsonNode actual = JsonUtils.readTree("{\"a\":2,\"c\":null,\"d\":1}");

        assertThat(JsonComparator.compare(expected, actual, Set.of(), false, true)).hasSize(4);
    }
}
