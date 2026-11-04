package com.restautomation.stepdefinitions.placesSteps;

import com.restautomation.responsevalidator.places.PlacesResponseValidator;
import com.restautomation.services.request.PlacesRequestService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

/** Steps specific to GetPlace.feature (also used by the other Places features to read a place back). */
@RequiredArgsConstructor
public class GetPlaceStepDefinitions {

    private final PlacesRequestService placesRequestService;
    private final PlacesResponseValidator placesResponseValidator;

    @Given("user has provided request to get the created place")
    public void requestToGetCreatedPlace() {
        placesRequestService.newRequest();
        placesRequestService.addPlaceIdQueryParam(placesRequestService.currentPlaceId());
    }

    @Given("user has provided request to get place with place_id {string}")
    public void requestToGetPlace(String placeId) {
        placesRequestService.newRequest();
        placesRequestService.addPlaceIdQueryParam(placeId);
    }

    @Then("GetPlace response should match the details sent in AddPlace request")
    public void getMatchesAddRequest() {
        placesResponseValidator.validateGetMatchesAddRequest();
    }

    @Then("GetPlace response should match test data {string} from file {string}")
    public void getMatchesTestData(String testCaseId, String file) {
        placesResponseValidator.validateGetMatchesTestData(testCaseId, file, placesRequestService.testData(file, testCaseId));
    }
}
