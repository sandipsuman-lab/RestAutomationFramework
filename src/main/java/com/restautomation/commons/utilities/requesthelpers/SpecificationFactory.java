package com.restautomation.commons.utilities.requesthelpers;

import com.restautomation.commons.endpoints.ApiEndpoints;
import com.restautomation.commons.endpoints.ReadEndpointsFromYAML;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.config.ReadConfigYaml;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * REST Assured request and response specifications, built once and reused.
 * <ul>
 *   <li>{@link #baseRequestSpec()} – every request: timeouts, shared Jackson mapper, SSL, logging filter</li>
 *   <li>{@link #apiRequestSpec(String)} – per API from endpoints.yml: base URI, default query params, headers</li>
 *   <li>{@link #responseSpec(int, DataType)} – expected status, response-time limit and, for APIs that
 *       declare it in endpoints.yml, the content type of successful responses</li>
 * </ul>
 * Built specifications are only read afterwards, so sharing them between parallel scenarios is safe.
 */
@Component
@RequiredArgsConstructor
public class SpecificationFactory {

    private final RestAssuredConfigFactory configFactory;
    private final RestAssuredFilter filter;
    private final ReadConfigYaml config;
    private final ReadEndpointsFromYAML endpointsYaml;

    private final Map<String, RequestSpecification> apiSpecs = new ConcurrentHashMap<>();
    private volatile RequestSpecification baseSpec;

    public RequestSpecification baseRequestSpec() {
        if (baseSpec == null) {
            synchronized (this) {
                if (baseSpec == null) {
                    baseSpec = new RequestSpecBuilder()
                            .setConfig(configFactory.create())
                            .addFilter(filter)
                            .setAccept(ContentType.JSON)
                            .build();
                }
            }
        }
        return baseSpec;
    }

    /** No filter here: it is merged on top of {@link #baseRequestSpec()}, which already logs. */
    public RequestSpecification apiRequestSpec(String apiName) {
        return apiSpecs.computeIfAbsent(apiName, name -> {
            ApiEndpoints api = endpointsYaml.getApi(name);
            return new RequestSpecBuilder()
                    .setConfig(configFactory.create())
                    .setBaseUri(api.getBaseUrl())
                    .addQueryParams(api.getQueryParams())
                    .addHeaders(api.getHeaders())
                    .setAccept(ContentType.JSON)
                    .build();
        });
    }

    public ResponseSpecification responseSpec(int expectedStatus, DataType type) {
        ResponseSpecBuilder builder = new ResponseSpecBuilder()
                .expectStatusCode(expectedStatus)
                .expectResponseTime(lessThanOrEqualTo((long) config.getHttp().getMaxResponseTimeMillis()), TimeUnit.MILLISECONDS);
        if (type != null && expectedStatus < 300) {
            endpointsYaml.findApi(type.getApiName())
                    .map(ApiEndpoints::getResponseContentType)
                    .filter(contentType -> !contentType.isBlank())
                    .ifPresent(contentType -> builder.expectHeader("Content-Type", containsString(contentType)));
        }
        return builder.build();
    }
}
