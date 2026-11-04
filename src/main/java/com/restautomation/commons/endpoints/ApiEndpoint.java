package com.restautomation.commons.endpoints;

import lombok.Data;

/** One named endpoint from endpoints.yml: HTTP method + resource path relative to the API's base URL. */
@Data
public class ApiEndpoint {
    private String method;
    private String resource;
    private String description;
}
