package com.restautomation.stepdefinitions.placesSteps;

import com.restautomation.responsevalidator.places.PlacesResponseValidator;
import com.restautomation.services.request.PlacesRequestService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

/** Steps specific to AddPlace.feature. */
@RequiredArgsConstructor
public class AddPlaceStepDefinitions {

    private final PlacesRequestService placesRequestService;
    private final PlacesResponseValidator placesResponseValidator;

    /** Data-driven body: the record whose testCaseId matches supplies the field values (YAML, JSON, CSV or Excel). */
    @Given("user has provided Places request body from template {string} using test data {string} from file {string}")
    public void bodyFromTestData(String template, String testCaseId, String file) {
        placesRequestService.newRequest();
        placesRequestService.setBodyFromTemplate(template, placesRequestService.testData(file, testCaseId));
    }

    @Then("place_id should be generated for the new place")
    public void placeIdGenerated() {
        placesResponseValidator.validatePlaceCreated();
    }
}
