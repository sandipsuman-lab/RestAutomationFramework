package com.restautomation.models.requestmodels.places;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Body of {@code POST maps/api/place/add/json}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddPlaceRequest {
    private Location location;
    private Integer accuracy;
    private String name;
    @JsonProperty("phone_number")
    private String phoneNumber;
    private String address;
    private List<String> types;
    private String website;
    private String language;
}
