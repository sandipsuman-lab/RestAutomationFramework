package com.restautomation.commons.utilities.parsers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.exceptions.FrameworkException;
import io.restassured.response.Response;

/** Extracts common values from HTTP responses. */
public final class ResponseParser {

    private ResponseParser() {
    }

    public static int getStatusCode(Response response) {
        return requireResponse(response).statusCode();
    }

    public static String getOperationId(Response response) {
        JsonNode node = JsonUtils.read(requireResponse(response).asString(), "$.operationId");
        return node == null ? null : node.asText();
    }

    public static JsonNode getBodyAsJson(Response response) {
        return JsonUtils.readTree(requireResponse(response).asString());
    }

    /** Value at a JsonPath, or null when the path is absent. */
    public static JsonNode getJsonPathValue(Response response, String jsonPath) {
        return JsonUtils.read(getBodyAsJson(response), jsonPath);
    }

    public static String getJsonPathText(Response response, String jsonPath) {
        JsonNode node = getJsonPathValue(response, jsonPath);
        return node == null || node.isNull() ? null : node.isValueNode() ? node.asText() : node.toString();
    }

    public static String getHeader(Response response, String name) {
        return requireResponse(response).getHeader(name);
    }

    private static Response requireResponse(Response response) {
        if (response == null) {
            throw new FrameworkException("No response available - was an endpoint called in this scenario?");
        }
        return response;
    }
}
