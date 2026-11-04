package com.restautomation.stepdefinitions.placesSteps;

import com.restautomation.services.request.PlacesRequestService;
import io.cucumber.java.en.Given;
import lombok.RequiredArgsConstructor;

/** Steps specific to DeletePlace.feature. The body is sent as a {@code DeletePlaceRequest} POJO. */
@RequiredArgsConstructor
public class DeletePlaceStepDefinitions {

    private final PlacesRequestService placesRequestService;

    @Given("user has provided DeletePlace request for the created place")
    public void deleteRequestForCreatedPlace() {
        String placeId = placesRequestService.currentPlaceId();
        placesRequestService.newRequest();
        placesRequestService.setDeletePlaceBody(placeId);
    }

    @Given("user has provided DeletePlace request for place_id {string}")
    public void deleteRequestForPlaceId(String placeId) {
        placesRequestService.usePlaceId(placeId);
        deleteRequestForCreatedPlace();
    }
}
