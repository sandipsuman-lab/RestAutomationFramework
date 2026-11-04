@Places @AddPlace
Feature: Validation of Add Place API
  As a consumer of the Places API
  I want to add new places to the server
  So that they can be retrieved, updated and deleted later

  Endpoint: AddPlaceAPI in endpoints.yml (method, resource, base URL and key)

  @testcase=PLC-101 @smoke
  Scenario: Verify a place is added successfully using the default request payload
    Given user has provided Places request body from template "addPlace.json"
    When user calls "AddPlaceAPI" with "POST" http request
    Then user should get response code 200
    And place_id should be generated for the new place
    And response field "status" should be "OK"
    And response field "scope" should be "APP"
    And user should be able to validate Places response using JSON file "placesSchema.json" and schema "addPlaceSuccess"
    And response should match JSON schema "addPlaceResponse.schema.json"
    And response header "Server" should be present
    And response time should be less than 5000 milliseconds

  @testcase=PLC-102 @regression
  Scenario Outline: Verify place "<name>" is added and persisted with the submitted details
    Given user has provided Places request body from template "addPlace.json" with overrides:
      | name     | <name>     |
      | address  | <address>  |
      | language | <language> |
    When user calls "AddPlaceAPI" with "POST" http request
    Then user should get response code 200
    And place_id should be generated for the new place
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And GetPlace response should match the details sent in AddPlace request

    Examples:
      | name                 | address                      | language   |
      | Frontline house      | 29, side layout, cohen 09    | French-IN  |
      | AA house             | World cross center           | English    |
      | Automation ${uuid}   | ${randomNumber:3} QA Street  | Spanish-ES |

  @testcase=PLC-103 @regression @dataDriven
  Scenario Outline: Verify place is added from external test data "<testCaseId>"
    Given user has provided Places request body from template "addPlace.json" using test data "<testCaseId>" from file "places/placesTestData.yml"
    When user calls "AddPlaceAPI" with "POST" http request
    Then user should get response code 200
    And place_id should be generated for the new place
    Given user has provided request to get the created place
    When user calls "GetPlaceAPI" with "GET" http request
    Then user should get response code 200
    And GetPlace response should match test data "<testCaseId>" from file "places/placesTestData.yml"

    Examples:
      | testCaseId  |
      | TC_PLACE_01 |
      | TC_PLACE_02 |
      | TC_PLACE_03 |
