@Places @GetPlace
Feature: Validation of Get Place API
  As a consumer of the Places API
  I want to fetch the details of an existing place
  So that I can verify what the server stored

  Endpoint: GetPlaceAPI in endpoints.yml (method, resource, base URL and key)

  @testcase=PLC-201 @smoke
  Scenario: Verify details of an added place are returned
    Given user has provided Places request body from template "addPlace.json"
    And user calls "AddPlaceAPI" with "POST" http request
    And place_id should be generated for the new place
    And user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And response should match JSON schema "getPlaceResponse.schema.json"
    And response field "name" should be "Frontline house"
    And response field "address" should be "29, side layout, cohen 09"
    # Includes the coordinates: documented as location.lat/lng, returned live as location.latitude/longitude
    And GetPlace response should match the details sent in AddPlace request

  @testcase=PLC-202 @negative
  Scenario Outline: Verify Get Place returns 404 for place_id "<placeId>"
    Given user has provided request to get place with place_id "<placeId>"
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 404
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "getPlaceNotFound"

    Examples:
      | placeId                          |
      | 00000000000000000000000000000000 |
      | invalid-place-id                 |
