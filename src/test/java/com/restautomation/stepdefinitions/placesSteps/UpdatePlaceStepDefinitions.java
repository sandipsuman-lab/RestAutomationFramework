package com.restautomation.stepdefinitions.placesSteps;

import com.restautomation.responsevalidator.places.PlacesResponseValidator;
import com.restautomation.services.request.PlacesRequestService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

/** Steps specific to UpdatePlace.feature. The body is sent as an {@code UpdatePlaceRequest} POJO. */
@RequiredArgsConstructor
public class UpdatePlaceStepDefinitions {

    private final PlacesRequestService placesRequestService;
    private final PlacesResponseValidator placesResponseValidator;

    /** Updates the current place_id (the one just added, or one given with "user has provided place_id"). */
    @Given("user has provided UpdatePlace request with new address {string}")
    public void updateRequestWithAddress(String address) {
        String placeId = placesRequestService.currentPlaceId();
        placesRequestService.newRequest();
        placesRequestService.addPlaceIdQueryParam(placeId);
        placesRequestService.setUpdatePlaceBody(placeId, address);
    }

    @Then("GetPlace response should show the updated address")
    public void getShowsUpdatedAddress() {
        placesResponseValidator.validateUpdatedAddress();
    }
}
