package com.restautomation.context;

/**
 * One HTTP call as seen on the wire, with secrets already masked. Recorded by the REST Assured
 * filter for reporting.
 */
public record HttpExchange(
        String method,
        String uri,
        String requestContentType,
        String requestBody,
        int statusCode,
        String responseContentType,
        String responseBody,
        long timeMillis) {
}
