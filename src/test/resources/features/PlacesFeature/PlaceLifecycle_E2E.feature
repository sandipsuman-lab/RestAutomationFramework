@Places @PlaceLifecycle @e2e
Feature: End-to-end lifecycle of a place
  Add -> Get -> Update -> Get -> Delete -> Get, chaining place_id between the calls.

  @testcase=PLC-501 @smoke @regression
  Scenario: Verify a place can be added, read, updated and deleted
    # Add
    Given user has provided Places request body from template "addPlace.json" with overrides:
      | name | Lifecycle place ${randomString:6} |
    When user calls "AddPlaceAPI" with "POST" http request
    Then user should get response code 200
    And place_id should be generated for the new place

    # Read back what was stored
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And GetPlace response should match the details sent in AddPlace request

    # Update the address
    Given user has provided UpdatePlace request with new address "${randomNumber:3} Lifecycle Road, USA"
    When user calls "UpdatePlaceAPI" with "PUT" http request
    Then user should get response code 200
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "updatePlaceSuccess"

    # Read back the update
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And GetPlace response should show the updated address

    # Delete (HTTP method taken from endpoints.yml)
    Given user has provided DeletePlace request for the created place
    When user calls "DeletePlaceAPI" endpoint
    Then user should get response code 200
    And response field "status" should be "OK"

    # Gone
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 404
