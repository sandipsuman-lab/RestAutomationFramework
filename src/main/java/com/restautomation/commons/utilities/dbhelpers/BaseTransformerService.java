package com.restautomation.commons.utilities.dbhelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.enumhelpers.DataType;

/** Converts one warehouse row (as JSON) into the DB model for a {@link DataType}. */
public interface BaseTransformerService<T> {

    DataType getDataType();

    T transform(JsonNode row);
}
