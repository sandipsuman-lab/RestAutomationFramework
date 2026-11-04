package com.restautomation.models.requestmodels.places;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Body of {@code POST maps/api/place/delete/json}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeletePlaceRequest {
    @JsonProperty("place_id")
    private String placeId;
}
