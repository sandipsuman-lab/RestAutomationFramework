package com.restautomation.commons.utilities.requesthelpers;

import io.restassured.RestAssured;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Low-level request builders on top of the shared base specification. Holds no state. */
@Component
@RequiredArgsConstructor
public class RequestSetup {

    private final SpecificationFactory specificationFactory;

    /** New request from the shared base specification (config, logging filter, JSON accept). */
    public RequestSpecification baseRequest() {
        return RestAssured.given().spec(specificationFactory.baseRequestSpec());
    }

    /** Base request plus the given headers. */
    public RequestSpecification generateRequest(Map<String, String> headers) {
        RequestSpecification spec = baseRequest();
        if (headers != null && !headers.isEmpty()) {
            spec.headers(headers);
        }
        return spec;
    }
}
