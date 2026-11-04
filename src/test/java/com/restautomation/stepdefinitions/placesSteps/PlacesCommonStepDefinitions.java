package com.restautomation.stepdefinitions.placesSteps;

import com.restautomation.commons.utilities.enumhelpers.RequestType;
import com.restautomation.reporting.PayloadReporter;
import com.restautomation.responsevalidator.common.ResponseValidator;
import com.restautomation.services.request.PlacesRequestService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Steps shared by every Places feature: request set-up from templates, calling the endpoints named in
 * endpoints.yml (AddPlaceAPI, GetPlaceAPI, UpdatePlaceAPI, DeletePlaceAPI), fixture validation and
 * clean-up of places created by the scenario. Every call's request and response payloads are written to
 * the calling step in the Spark report and attached to the Cucumber report.
 */
@RequiredArgsConstructor
public class PlacesCommonStepDefinitions {

    private final PlacesRequestService placesRequestService;
    private final ResponseValidator responseValidator;
    private final PayloadReporter payloadReporter;

    @Given("user has provided request details for Places API")
    public void requestDetails() {
        placesRequestService.newRequest();
    }

    @Given("user has provided Places request body from template {string}")
    public void bodyFromTemplate(String template) {
        placesRequestService.newRequest();
        placesRequestService.setBodyFromTemplate(template, Map.of());
    }

    /** Two-column table without header: field / JsonPath | value ({@code <remove>}, {@code <null>}, ${...} supported). */
    @Given("user has provided Places request body from template {string} with overrides:")
    public void bodyFromTemplateWithOverrides(String template, DataTable overrides) {
        placesRequestService.newRequest();
        placesRequestService.setBodyFromTemplate(template, overrides.asMap(String.class, String.class));
    }

    /** Points later ${ctx:placeId} templates at an arbitrary (e.g. non-existent) place. */
    @Given("user has provided place_id {string}")
    public void userHasProvidedPlaceId(String placeId) {
        placesRequestService.usePlaceId(placeId);
    }

    /** Endpoint name from endpoints.yml; the method given here is used even if YAML says otherwise. */
    @When("user calls {string} with {string} http request")
    public void userCallsWithMethod(String endpointName, String method) {
        placesRequestService.call(endpointName, RequestType.fromName(method));
        payloadReporter.reportLastExchange(endpointName);
    }

    /** Endpoint name from endpoints.yml, called with the method configured there. */
    @When("user calls {string} endpoint")
    public void userCallsEndpoint(String endpointName) {
        placesRequestService.call(endpointName, null);
        payloadReporter.reportLastExchange(endpointName);
    }

    @Then("user should be able to validate Places response using JSON file {string} and schema {string}")
    public void validateFixture(String file, String key) {
        responseValidator.validateAgainstFixture(PlacesRequestService.TYPE, file, key);
    }

    /** Runs after Hooks.tearDown (order 100) so failure attachments show the scenario's last call. */
    @After(value = "@Places", order = 10)
    public void deleteCreatedPlaces() {
        placesRequestService.deleteCreatedPlaces();
    }
}
