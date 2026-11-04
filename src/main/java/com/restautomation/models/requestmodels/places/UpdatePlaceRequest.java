package com.restautomation.models.requestmodels.places;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of {@code PUT maps/api/place/update/json}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePlaceRequest {
    @JsonProperty("place_id")
    private String placeId;
    private String address;
    private String key;
}
