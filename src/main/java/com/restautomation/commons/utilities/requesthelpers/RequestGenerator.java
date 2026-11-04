package com.restautomation.commons.utilities.requesthelpers;

import com.restautomation.commons.endpoints.ApiEndpoint;
import com.restautomation.commons.endpoints.ApiEndpoints;
import com.restautomation.commons.endpoints.ReadEndpointsFromYAML;
import com.restautomation.commons.utilities.common.UrlUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.enumhelpers.RequestType;
import com.restautomation.context.ScenarioContext;
import com.restautomation.exceptions.ConfigurationException;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * High-level request API used by step definitions. An API is selected from endpoints.yml, steps
 * accumulate headers, params and body on the {@link ScenarioContext}, and a named endpoint is called.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestGenerator {

    private final ScenarioContext context;
    private final RequestSetup requestSetup;
    private final ReadEndpointsFromYAML endpointsYaml;
    private final SpecificationFactory specificationFactory;

    /**
     * Selects an API from endpoints.yml. Its base URI, default query params (e.g. an API key) and headers
     * come from a REST Assured {@link RequestSpecification} built by {@link SpecificationFactory}.
     */
    public void selectApi(DataType type) {
        ApiEndpoints api = endpointsYaml.getApi(type.getApiName());
        if (api.getBaseUrl() == null || api.getBaseUrl().isBlank()) {
            throw new ConfigurationException("No baseUrl for apiName '" + type.getApiName() + "' in endpoints.yml");
        }
        context.setDataType(type);
        context.getHeaders().clear();
        context.setApiSpecification(specificationFactory.apiRequestSpec(type.getApiName()));
    }

    public void addHeader(String name, String value) {
        context.getHeaders().put(name, value);
    }

    public void addQueryParam(String name, Object value) {
        context.getQueryParams().put(name, value);
    }

    public void addQueryParams(Map<String, ?> params) {
        context.getQueryParams().putAll(params);
    }

    public void addPathParam(String name, Object value) {
        context.getPathParams().put(name, value);
    }

    /** POJO, Map or JSON string; POJOs and maps are serialised by REST Assured. */
    public void setRequestBody(Object body) {
        context.setRequestBody(body);
    }

    /**
     * Calls a named endpoint from endpoints.yml. {@code methodOverride} (e.g. from the feature file) wins
     * over the configured method, which allows wrong-method negative tests; pass null to use the YAML method.
     */
    public Response callNamedEndpoint(DataType type, String endpointName, RequestType methodOverride) {
        ApiEndpoint endpoint = endpointsYaml.getEndpoint(type.getApiName(), endpointName);
        RequestType method = methodOverride != null ? methodOverride : RequestType.fromName(endpoint.getMethod());
        if (methodOverride != null && !methodOverride.name().equalsIgnoreCase(endpoint.getMethod())) {
            log.info("{} is configured as {} but called with {}", endpointName, endpoint.getMethod(), methodOverride);
        }
        return generateResponseBasedOnEndPoint(endpoint.getResource(), method);
    }

    /**
     * Sends the accumulated request to {@code endpoint}: a resource relative to the selected API's base URI,
     * or an absolute URL. The response is stored on the scenario context.
     */
    public Response generateResponseBasedOnEndPoint(String endpoint, RequestType requestType) {
        if (context.getApiSpecification() == null && !UrlUtils.isAbsolute(endpoint)) {
            throw new ConfigurationException("No API selected for this scenario; cannot resolve '" + endpoint + "'");
        }
        RequestSpecification spec = requestSetup.generateRequest(context.getHeaders());
        if (context.getApiSpecification() != null) {
            spec.spec(context.getApiSpecification());
        }
        spec.queryParams(context.getQueryParams()).pathParams(context.getPathParams());
        Object body = context.getRequestBody();
        if (body != null) {
            spec.contentType(ContentType.JSON).body(body);
        }
        Response response = switch (requestType) {
            case Get -> spec.get(endpoint);
            case Post -> spec.post(endpoint);
            case Put -> spec.put(endpoint);
            case Patch -> spec.patch(endpoint);
            case Delete -> spec.delete(endpoint);
            case Head -> spec.head(endpoint);
            case Options -> spec.options(endpoint);
        };
        context.setResponse(response);
        return response;
    }
}
