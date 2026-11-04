package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.enumhelpers.EndPoints;
import com.restautomation.exceptions.ConfigurationException;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps an HTTP response to its model. The default model comes from {@link DataType#getResponseClass()};
 * endpoints whose payload differs register an override with {@link #register}.
 */
@Component
public class ResponseToClassObjectParser {

    private final Map<String, Class<?>> overrides = new ConcurrentHashMap<>();

    public void register(DataType type, EndPoints endpoint, Class<?> modelClass) {
        overrides.put(key(type, endpoint), modelClass);
    }

    @SuppressWarnings("unchecked")
    public <T> T parse(Response response, DataType type) {
        return (T) parse(response, modelFor(type, null));
    }

    @SuppressWarnings("unchecked")
    public <T> T parse(Response response, DataType type, EndPoints endpoint) {
        return (T) parse(response, modelFor(type, endpoint));
    }

    public <T> T parse(Response response, Class<T> modelClass) {
        return JsonUtils.fromJson(ResponseParser.getBodyAsJson(response).toString(), modelClass);
    }

    private Class<?> modelFor(DataType type, EndPoints endpoint) {
        Class<?> model = endpoint == null ? null : overrides.get(key(type, endpoint));
        if (model == null) {
            model = type.getResponseClass();
        }
        if (model == null) {
            throw new ConfigurationException("No response model configured for " + type + (endpoint == null ? "" : "/" + endpoint));
        }
        return model;
    }

    private static String key(DataType type, EndPoints endpoint) {
        return type.name() + "/" + endpoint.name();
    }
}
