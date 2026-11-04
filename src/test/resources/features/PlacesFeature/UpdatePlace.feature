@Places @UpdatePlace
Feature: Validation of Update Place API
  As a consumer of the Places API
  I want to update the address of an existing place
  So that the server holds current information

  Endpoint: UpdatePlaceAPI in endpoints.yml (method, resource, base URL and key)

  Background:
    Given user has provided Places request body from template "addPlace.json"
    And user calls "AddPlaceAPI" with "POST" http request
    And place_id should be generated for the new place

  @testcase=PLC-301 @smoke
  Scenario: Verify the address of a place is updated
    Given user has provided UpdatePlace request with new address "70 winter walk, USA"
    When user calls "UpdatePlaceAPI" with "PUT" http request
    Then user should get response code 200
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "updatePlaceSuccess"
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And GetPlace response should show the updated address
    And response field "name" should be "Frontline house"
    And response field "language" should be "French-IN"

  @testcase=PLC-302 @regression
  Scenario Outline: Verify the address can be updated to "<newAddress>"
    Given user has provided UpdatePlace request with new address "<newAddress>"
    When user calls "UpdatePlaceAPI" with "PUT" http request
    Then user should get response code 200
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then GetPlace response should show the updated address

    Examples:
      | newAddress                       |
      | 221B Baker Street, London        |
      | ${randomNumber:4} Automation Ave |

  @testcase=PLC-303 @negative
  Scenario: Verify updating a non-existent place is rejected
    Given user has provided place_id "00000000000000000000000000000000"
    And user has provided UpdatePlace request with new address "Nowhere road"
    When user calls "UpdatePlaceAPI" with "PUT" http request
    Then user should get response code 404
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "updatePlaceNotFound"
