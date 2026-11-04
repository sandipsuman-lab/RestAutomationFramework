package com.restautomation.commons.endpoints;

import com.restautomation.exceptions.ConfigurationException;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/** Everything needed to call one API: base URL, query params sent on every call, named endpoints. */
@Data
public class ApiEndpoints {
    private String apiName;
    private String baseUrl;
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> queryParams = new LinkedHashMap<>();
    /** Content type every successful response must have (checked by the response specification). */
    private String responseContentType;
    private Map<String, ApiEndpoint> endpoints = new LinkedHashMap<>();

    public ApiEndpoint getEndpoint(String name) {
        ApiEndpoint endpoint = endpoints.get(name);
        if (endpoint == null) {
            throw new ConfigurationException("No endpoint '" + name + "' for apiName '" + apiName
                    + "' in endpoints.yml. Available: " + endpoints.keySet());
        }
        return endpoint;
    }
}
