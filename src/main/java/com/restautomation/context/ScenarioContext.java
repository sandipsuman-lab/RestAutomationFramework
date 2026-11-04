package com.restautomation.context;

import com.restautomation.commons.utilities.enumhelpers.DataType;
import io.cucumber.java.Scenario;
import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-scenario state. A new instance is created for every scenario (and every thread when running
 * in parallel), so framework singletons can inject it without sharing state between scenarios.
 */
@Getter
@Setter
@Component
@ScenarioScope
public class ScenarioContext {

    private Scenario scenario;
    private DataType dataType;
    /** REST Assured spec of the API selected from endpoints.yml (base URI, default query params, headers). */
    private RequestSpecification apiSpecification;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Object> queryParams = new LinkedHashMap<>();
    private final Map<String, Object> pathParams = new LinkedHashMap<>();
    private Object requestBody;
    private Response response;
    private long lastResponseTimeMillis;
    private String lastRequestLog;
    private String lastResponseLog;
    private HttpExchange lastExchange;

    /** Free-form store for values shared between steps; readable in templates as ${ctx:key}. */
    private final Map<String, Object> store = new ConcurrentHashMap<>();

    public void put(String key, Object value) {
        if (value == null) {
            store.remove(key);
        } else {
            store.put(key, value);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) store.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = store.get(key);
        return value == null ? null : type.cast(value);
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    /** Clears request-building state so a step can start a fresh request in the same scenario. */
    public void resetRequest() {
        headers.clear();
        queryParams.clear();
        pathParams.clear();
        requestBody = null;
    }

    public String getScenarioName() {
        return scenario != null ? scenario.getName() : "no-scenario";
    }
}
