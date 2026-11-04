package com.restautomation.commons.utilities.enumhelpers;

import com.restautomation.exceptions.ConfigurationException;
import com.restautomation.models.responsefromjson.places.AddPlaceResponse;
import lombok.Getter;

import java.util.Arrays;

/**
 * Identifies an API / payload type and carries what the framework needs to route it: the
 * {@code apiName} used as key in endpoints.yml and queries.yml, the default response model and the
 * fixture folders. Adding an API = adding a constant here.
 */
@Getter
public enum DataType {

    Places("places", AddPlaceResponse.class, "PlacesSchema", "PlacesRequest"),
    /**
     * Warehouse (DB) example kept for future data validation: queries.yml "getItems" queries,
     * ItemsController, ItemsUtilityService and ItemsDbRecord. Not used by the Places tests.
     */
    GetItems("getItems", null, null, null);

    private final String apiName;
    private final Class<?> responseClass;
    private final String schemaFolder;
    private final String requestFolder;

    DataType(String apiName, Class<?> responseClass, String schemaFolder, String requestFolder) {
        this.apiName = apiName;
        this.responseClass = responseClass;
        this.schemaFolder = schemaFolder;
        this.requestFolder = requestFolder;
    }

    /** Accepts the enum name ({@code Places}) or the apiName ({@code places}), case-insensitively. */
    public static DataType fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name) || type.apiName.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unknown DataType '" + name + "'. Known: " + Arrays.toString(values())));
    }
}
