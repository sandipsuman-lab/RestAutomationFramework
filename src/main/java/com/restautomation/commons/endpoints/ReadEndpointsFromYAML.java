package com.restautomation.commons.endpoints;

import com.restautomation.commons.utilities.readerhelpers.YamlResourceReader;
import com.restautomation.exceptions.ConfigurationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loads {@code endpoints.yml} into {@code Map<apiName, ApiEndpoints>} (cached). */
@Component
@RequiredArgsConstructor
public class ReadEndpointsFromYAML {

    public static final String ENDPOINTS_FILE = "endpoints.yml";

    private final YamlResourceReader yamlReader;
    private volatile Map<String, ApiEndpoints> apis;

    public Map<String, ApiEndpoints> getApis() {
        if (apis == null) {
            synchronized (this) {
                if (apis == null) {
                    Map<String, ApiEndpoints> map = new LinkedHashMap<>();
                    yamlReader.read(ENDPOINTS_FILE, EndpointsYaml.class).getApis()
                            .forEach(api -> map.put(api.getApiName(), api));
                    apis = map;
                }
            }
        }
        return apis;
    }

    public ApiEndpoints getApi(String apiName) {
        ApiEndpoints api = getApis().get(apiName);
        if (api == null) {
            throw new ConfigurationException("No apiName '" + apiName + "' in " + ENDPOINTS_FILE + ". Configured: " + getApis().keySet());
        }
        return api;
    }

    public Optional<ApiEndpoints> findApi(String apiName) {
        return Optional.ofNullable(getApis().get(apiName));
    }

    public ApiEndpoint getEndpoint(String apiName, String endpointName) {
        return getApi(apiName).getEndpoint(endpointName);
    }

    @Data
    public static class EndpointsYaml {
        private List<ApiEndpoints> apis = new ArrayList<>();
    }
}
