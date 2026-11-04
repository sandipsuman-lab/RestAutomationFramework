@Places @DeletePlace
Feature: Validation of Delete Place API
  As a consumer of the Places API
  I want to delete places I no longer need
  So that they are removed from the server

  Endpoint: DeletePlaceAPI in endpoints.yml (method, resource, base URL and key)

  @testcase=PLC-401 @smoke
  Scenario: Verify an existing place is deleted and can no longer be retrieved
    Given user has provided Places request body from template "addPlace.json"
    And user calls "AddPlaceAPI" with "POST" http request
    And place_id should be generated for the new place
    Given user has provided DeletePlace request for the created place
    When user calls "DeletePlaceAPI" with "POST" http request
    Then user should get response code 200
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "deletePlaceSuccess"
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 404
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "getPlaceNotFound"

  @testcase=PLC-402 @negative
  Scenario: Verify deleting the same place twice is rejected
    Given user has provided Places request body from template "addPlace.json"
    And user calls "AddPlaceAPI" with "POST" http request
    And user has provided DeletePlace request for the created place
    And user calls "DeletePlaceAPI" with "POST" http request
    And user should get response code 200
    Given user has provided DeletePlace request for the created place
    When user calls "DeletePlaceAPI" with "POST" http request
    Then user should get response code 404
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "deletePlaceNotFound"

  @testcase=PLC-403 @negative
  Scenario: Verify deleting a non-existent place is rejected
    Given user has provided DeletePlace request for place_id "00000000000000000000000000000000"
    When user calls "DeletePlaceAPI" with "POST" http request
    Then user should get response code 404
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "deletePlaceNotFound"
