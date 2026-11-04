package com.restautomation.models.responsefromjson.places;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Response of {@code POST maps/api/place/add/json}. */
@Data
public class AddPlaceResponse {
    private String status;
    @JsonProperty("place_id")
    private String placeId;
    private String scope;
    private String reference;
    private String id;
}
