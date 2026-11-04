package com.restautomation.stepdefinitions.common;

import com.restautomation.commons.utilities.parsers.ResponseParser;
import com.restautomation.context.ScenarioContext;
import com.restautomation.responsevalidator.common.ResponseValidator;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

/** API-agnostic response steps usable from any feature. */
@RequiredArgsConstructor
public class CommonStepDefinitions {

    private final ResponseValidator responseValidator;
    private final ScenarioContext context;

    @Then("user should get response code {int}")
    public void userShouldGetResponseCode(int expected) {
        responseValidator.validateResponseCode(expected);
    }

    @Then("response should match JSON schema {string}")
    public void responseShouldMatchJsonSchema(String schemaFile) {
        responseValidator.validateJsonSchema(schemaFile);
    }

    @Then("response time should be less than {long} milliseconds")
    public void responseTimeShouldBeLessThan(long maxMillis) {
        responseValidator.validateResponseTime(maxMillis);
    }

    @Then("response field {string} should be {string}")
    public void responseFieldShouldBe(String jsonPath, String expected) {
        responseValidator.validateJsonPathValue(jsonPath, expected);
    }

    @Then("response field {string} should not be empty")
    public void responseFieldShouldNotBeEmpty(String jsonPath) {
        responseValidator.validateJsonPathNotEmpty(jsonPath);
    }

    @Then("response list {string} should have {int} elements")
    public void responseListShouldHaveElements(String jsonPath, int size) {
        responseValidator.validateArraySize(jsonPath, size);
    }

    @Then("response header {string} should be present")
    public void responseHeaderShouldBePresent(String header) {
        responseValidator.validateHeaderPresent(header);
    }

    @Given("user stores response field {string} as {string}")
    public void userStoresResponseField(String jsonPath, String key) {
        context.put(key, ResponseParser.getJsonPathText(context.getResponse(), jsonPath));
    }
}
