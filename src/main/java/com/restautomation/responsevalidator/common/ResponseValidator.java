package com.restautomation.responsevalidator.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonComparator;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.parsers.DataConversionHelper;
import com.restautomation.commons.utilities.parsers.ResponseParser;
import com.restautomation.commons.utilities.parsers.SchemaValidatorParser;
import com.restautomation.commons.utilities.readerhelpers.ReadJsonFile;
import com.restautomation.commons.utilities.requesthelpers.SpecificationFactory;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.context.ScenarioContext;
import com.restautomation.exceptions.FrameworkException;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reusable response checks: response specification (status, time, content type), expected-response
 * fixtures, JSON Schema contracts, headers, JsonPath values and response time.
 */
@Component
@RequiredArgsConstructor
public class ResponseValidator {

    private final ScenarioContext context;
    private final ReadJsonFile readJsonFile;
    private final SchemaValidatorParser schemaValidatorParser;
    private final SpecificationFactory specificationFactory;
    private final ReadConfigYaml config;

    /**
     * Validates the response against a REST Assured {@link io.restassured.specification.ResponseSpecification}:
     * status code, response-time limit and (for 2xx of APIs that declare one in endpoints.yml) content type.
     */
    public void validateResponseCode(int expected) {
        Response response = response();
        try {
            response.then().spec(specificationFactory.responseSpec(expected, context.getDataType()));
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage().trim() + "\nResponse body: " + abbreviate(response.asString()), e);
        }
    }

    /** Compares the response body with fixture {@code key} in {@code schemas/<type folder>/<file>}. */
    public void validateAgainstFixture(DataType type, String file, String key) {
        validateAgainstFixture(schemaValidatorParser.getSchemaFolder(type), file, key, Set.of());
    }

    public void validateAgainstFixture(String folder, String file, String key, Set<String> extraIgnoredFields) {
        JsonNode expected = readJsonFile.getJsonData(folder, file, key);
        JsonNode actual = ResponseParser.getBodyAsJson(response());
        Set<String> ignored = new HashSet<>(config.getValidation().getDynamicFields());
        ignored.addAll(extraIgnoredFields);
        List<String> differences = JsonComparator.compare(expected, actual, ignored);
        if (!differences.isEmpty()) {
            throw new AssertionError("Response does not match fixture '" + key + "' in " + folder + "/" + file + ":\n  - "
                    + String.join("\n  - ", differences) + "\nActual body: " + abbreviate(actual.toString()));
        }
    }

    /** Contract validation against a real JSON Schema document on the classpath. */
    public void validateJsonSchema(String schemaFile) {
        response().then().assertThat()
                .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schemaValidatorParser.getJsonSchemaPath(schemaFile)));
    }

    public void validateResponseTime(long maxMillis) {
        assertThat(context.getLastResponseTimeMillis()).as("response time (ms)").isLessThanOrEqualTo(maxMillis);
    }

    public void validateHeaderPresent(String headerName) {
        assertThat(response().getHeader(headerName)).as("response header %s", headerName).isNotBlank();
    }

    public void validateContentType(String expected) {
        assertThat(response().getContentType()).as("Content-Type").containsIgnoringCase(expected);
    }

    public void validateJsonPathValue(String jsonPath, String expected) {
        String actual = ResponseParser.getJsonPathText(response(), jsonPath);
        assertThat(DataConversionHelper.valuesEqual(expected, actual))
                .as("value at %s: expected [%s] but was [%s]", jsonPath, expected, actual)
                .isTrue();
    }

    public void validateJsonPathNotEmpty(String jsonPath) {
        JsonNode node = ResponseParser.getJsonPathValue(response(), jsonPath);
        boolean empty = node == null || node.isNull() || (node.isContainerNode() ? node.isEmpty() : node.asText().isBlank());
        assertThat(empty).as("value at %s should not be empty but was %s", jsonPath, node).isFalse();
    }

    public void validateArraySize(String jsonPath, int expectedSize) {
        JsonNode node = ResponseParser.getJsonPathValue(response(), jsonPath);
        assertThat(node).as("array at %s", jsonPath).isNotNull();
        assertThat(node.size()).as("size of %s", jsonPath).isEqualTo(expectedSize);
    }

    private Response response() {
        Response response = context.getResponse();
        if (response == null) {
            throw new FrameworkException("No response available - was an endpoint called in this scenario?");
        }
        return response;
    }

    private static String abbreviate(String text) {
        return text != null && text.length() > 1000 ? text.substring(0, 1000) + "..." : text;
    }
}
