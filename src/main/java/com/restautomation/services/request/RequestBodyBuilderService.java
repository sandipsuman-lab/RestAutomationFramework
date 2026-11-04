package com.restautomation.services.request;

import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.parsers.RequestToClassObjectParser;
import com.restautomation.models.requestmodels.places.AddPlaceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Builds write-back request bodies from the templates under {@code requestjsonfiles/}. */
@Service
@RequiredArgsConstructor
public class RequestBodyBuilderService {

    private final RequestToClassObjectParser requestParser;

    /** Raw JSON body; use when a test must send fields the model does not know (or omit required ones). */
    public String buildRequestBody(DataType type, String templateFile, Map<String, ?> overrides) {
        return requestParser.buildJson(type, templateFile, overrides);
    }

    /** Template + overrides mapped onto a request POJO; REST Assured serialises it when sent. */
    public <T> T buildRequest(DataType type, String templateFile, Map<String, ?> overrides, Class<T> modelClass) {
        return requestParser.parse(type, templateFile, modelClass, overrides);
    }

    public AddPlaceRequest buildAddPlaceRequest(String templateFile, Map<String, ?> overrides) {
        return buildRequest(DataType.Places, templateFile, overrides, AddPlaceRequest.class);
    }
}
