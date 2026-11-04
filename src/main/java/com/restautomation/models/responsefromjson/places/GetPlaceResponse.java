package com.restautomation.models.responsefromjson.places;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.restautomation.models.requestmodels.places.Location;
import lombok.Data;

/**
 * Response of {@code GET maps/api/place/get/json}. The live service returns numbers as strings and
 * {@code types} as a comma-joined string, hence the loose types.
 */
@Data
public class GetPlaceResponse {
    private Location location;
    private String accuracy;
    private String name;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String address;
    private Object types;
    private String website;
    private String language;
}
