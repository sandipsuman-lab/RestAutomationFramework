package com.restautomation.commons.utilities.dbhelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.exceptions.FrameworkException;

/** Default transformer: Jackson-maps the row onto {@code modelClass}. Override {@link #postProcess} for custom logic. */
public abstract class AbstractTransformerService<T> implements BaseTransformerService<T> {

    private final Class<T> modelClass;

    protected AbstractTransformerService(Class<T> modelClass) {
        this.modelClass = modelClass;
    }

    @Override
    public T transform(JsonNode row) {
        try {
            return postProcess(JsonUtils.mapper().treeToValue(row, modelClass), row);
        } catch (Exception e) {
            throw new FrameworkException("Unable to map DB row to " + modelClass.getSimpleName() + ": " + row, e);
        }
    }

    protected T postProcess(T model, JsonNode row) {
        return model;
    }
}
