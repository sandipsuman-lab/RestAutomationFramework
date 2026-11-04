package com.restautomation.models.requestmodels.places;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Sent as lat/lng; the live GET answers with latitude/longitude, accepted via aliases. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Location {
    @JsonAlias("latitude")
    private BigDecimal lat;
    @JsonAlias("longitude")
    private BigDecimal lng;
}
