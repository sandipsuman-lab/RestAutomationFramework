# Places API: everything in the framework

This page lists every file, setting, step and test in this framework that belongs to the **Places API** suite
(Add / Get / Update / Delete Place), and shows how they connect. For the framework as a whole see
[README.md](README.md) and [ARCHITECTURE.md](ARCHITECTURE.md).

## The API under test

| | |
|---|---|
| Base URL | `https://rahulshettyacademy.com` |
| API key | `key=qaclick123` query parameter, added to every call |

Every run calls this production URL. There is no local stub.

| Endpoint name (used in steps) | Method | Resource | `place_id` sent as |
|---|---|---|---|
| `AddPlaceAPI` | POST | `/maps/api/place/add/json` | returned in the response |
| `GetPlaceAPI` | GET | `/maps/api/place/get/json` | query parameter |
| `UpdatePlaceAPI` | PUT | `/maps/api/place/update/json` | request body |
| `DeletePlaceAPI` | POST | `/maps/api/place/delete/json` | request body |

## File map

### Configuration

| File | What it holds for Places |
|---|---|
| [src/test/resources/endpoints.yml](src/test/resources/endpoints.yml) | The `places` API entry: base URL, `key` query param, response content type, and the four endpoints above |
| [src/test/resources/application.yml](src/test/resources/application.yml) | `places.base-url` (production) and `places.api-key` |
| [src/test/resources/config.yml](src/test/resources/config.yml) | HTTP timeouts, connection retries, max response time, logging and masking used by every Places call |
| [src/main/java/com/restautomation/commons/utilities/enumhelpers/DataType.java](src/main/java/com/restautomation/commons/utilities/enumhelpers/DataType.java) | `DataType.Places`: links the `places` API name to its response class, fixture folder (`PlacesSchema`) and request folder (`PlacesRequest`) |

### Feature files: `src/test/resources/features/PlacesFeature/`

| File | Feature tag |
|---|---|
| [AddPlace.feature](src/test/resources/features/PlacesFeature/AddPlace.feature) | `@AddPlace` |
| [GetPlace.feature](src/test/resources/features/PlacesFeature/GetPlace.feature) | `@GetPlace` |
| [UpdatePlace.feature](src/test/resources/features/PlacesFeature/UpdatePlace.feature) | `@UpdatePlace` |
| [DeletePlace.feature](src/test/resources/features/PlacesFeature/DeletePlace.feature) | `@DeletePlace` |
| [PlaceLifecycle_E2E.feature](src/test/resources/features/PlacesFeature/PlaceLifecycle_E2E.feature) | `@PlaceLifecycle @e2e` |

Every feature also carries `@Places`. The scenarios are listed in [Test scenarios](#test-scenarios).

### Step definitions: `src/test/java/com/restautomation/stepdefinitions/placesSteps/`

| Class | Steps it provides |
|---|---|
| [PlacesCommonStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/placesSteps/PlacesCommonStepDefinitions.java) | Request body from a template (with or without overrides), setting a `place_id`, calling an endpoint, fixture validation, and the `@After("@Places")` clean-up hook |
| [AddPlaceStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/placesSteps/AddPlaceStepDefinitions.java) | Body from external test data; `place_id should be generated` |
| [GetPlaceStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/placesSteps/GetPlaceStepDefinitions.java) | Get request for the created or a given place; read-back comparison with the Add request or test data |
| [UpdatePlaceStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/placesSteps/UpdatePlaceStepDefinitions.java) | Update request with a new address; read-back check of the address |
| [DeletePlaceStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/placesSteps/DeletePlaceStepDefinitions.java) | Delete request for the created or a given place |

### Service and validation

| Class | Role |
|---|---|
| [PlacesRequestService.java](src/main/java/com/restautomation/services/request/PlacesRequestService.java) | Builds the request bodies, calls the endpoints, keeps the current `place_id` in the scenario context, tracks every created place and deletes the ones a scenario leaves behind |
| [RequestBodyBuilderService.java](src/main/java/com/restautomation/services/request/RequestBodyBuilderService.java) | `buildAddPlaceRequest`: reads a template from `requestjsonfiles/PlacesRequest/` and applies overrides |
| [PlacesResponseValidator.java](src/main/java/com/restautomation/responsevalidator/places/PlacesResponseValidator.java) | Checks a place was created, compares Get responses with the Add request or test data field by field, and checks an updated address |

### Request and response models (POJOs)

| Folder | Classes |
|---|---|
| [models/requestmodels/places/](src/main/java/com/restautomation/models/requestmodels/places/) | `AddPlaceRequest`, `Location`, `UpdatePlaceRequest`, `DeletePlaceRequest` |
| [models/responsefromjson/places/](src/main/java/com/restautomation/models/responsefromjson/places/) | `AddPlaceResponse`, `GetPlaceResponse`, `PlaceMessageResponse` (the `msg` body of Update / Delete / 404 responses) |

### Test data, templates and expected responses

| File | Used for |
|---|---|
| [requestjsonfiles/PlacesRequest/addPlace.json](src/test/resources/requestjsonfiles/PlacesRequest/addPlace.json) | Default Add Place body ("Frontline house"); steps override fields from it |
| [requestjsonfiles/PlacesRequest/updatePlace.json](src/test/resources/requestjsonfiles/PlacesRequest/updatePlace.json) | Update body; `place_id` and `key` filled from `${ctx:placeId}` and `${prop:places.api-key}` |
| [requestjsonfiles/PlacesRequest/deletePlace.json](src/test/resources/requestjsonfiles/PlacesRequest/deletePlace.json) | Delete body; `place_id` filled from `${ctx:placeId}` |
| [testdata/places/placesTestData.yml](src/test/resources/testdata/places/placesTestData.yml) | Records `TC_PLACE_01`–`TC_PLACE_03` for the data-driven Add Place scenario |
| [schemas/PlacesSchema/placesSchema.json](src/test/resources/schemas/PlacesSchema/placesSchema.json) | Expected-response fixtures: `addPlaceSuccess`, `updatePlaceSuccess`, `deletePlaceSuccess`, `getPlaceNotFound`, `updatePlaceNotFound`, `deletePlaceNotFound` |
| [jsonschema/addPlaceResponse.schema.json](src/test/resources/jsonschema/addPlaceResponse.schema.json) | JSON Schema for the Add Place response |
| [jsonschema/getPlaceResponse.schema.json](src/test/resources/jsonschema/getPlaceResponse.schema.json) | JSON Schema for the Get Place response |

### Shared framework pieces the Places suite relies on

These are not Places-specific, but every Places call goes through them.

| Class | Role |
|---|---|
| [CommonStepDefinitions.java](src/test/java/com/restautomation/stepdefinitions/common/CommonStepDefinitions.java) | Generic checks: response code, JSON Schema, response time, field values, headers |
| [RequestGenerator.java](src/main/java/com/restautomation/commons/utilities/requesthelpers/RequestGenerator.java) | Resolves an endpoint name from `endpoints.yml` and sends the request |
| [SpecificationFactory.java](src/main/java/com/restautomation/commons/utilities/requesthelpers/SpecificationFactory.java) | Request specs (base URI, key, headers) and the response spec behind `user should get response code N` |
| [RestAssuredConfigFactory.java](src/main/java/com/restautomation/commons/utilities/requesthelpers/RestAssuredConfigFactory.java) | Timeouts, SSL, and the retry when a connection to the server cannot be opened |
| [RestAssuredFilter.java](src/main/java/com/restautomation/commons/utilities/requesthelpers/RestAssuredFilter.java) | Logs error responses and failed calls with secrets masked, and records every call for the reports |
| [Hooks.java](src/test/java/com/restautomation/hooks/Hooks.java) | One result line per scenario in the log; on failure, logs the last call and attaches it to the report |

## Test scenarios

18 scenarios in total, counting each Examples row once.

| ID | Feature | Scenario | Tags | Runs |
|---|---|---|---|---|
| PLC-101 | Add | Place added with the default payload; status, scope, fixture, JSON Schema, `Server` header, response time under 5 s | `@smoke` | 1 |
| PLC-102 | Add | Place added with a given name, address and language, then read back | `@regression` | 3 |
| PLC-103 | Add | Place added from external test data, then read back | `@regression @dataDriven` | 3 |
| PLC-201 | Get | Every field of an added place is returned | `@smoke` | 1 |
| PLC-202 | Get | 404 for an unknown or invalid `place_id` | `@negative` | 2 |
| PLC-301 | Update | Address updated and visible on read-back; name and language unchanged | `@smoke` | 1 |
| PLC-302 | Update | Address updated to different values | `@regression` | 2 |
| PLC-303 | Update | Updating an unknown place is rejected (404) | `@negative` | 1 |
| PLC-401 | Delete | Deleted place returns 404 on Get | `@smoke` | 1 |
| PLC-402 | Delete | Deleting the same place twice is rejected | `@negative` | 1 |
| PLC-403 | Delete | Deleting an unknown place is rejected | `@negative` | 1 |
| PLC-501 | Lifecycle | Add → Get → Update → Get → Delete → Get | `@smoke @regression @e2e` | 1 |

## How a Places scenario runs

1. **Build the body.** The template from `requestjsonfiles/PlacesRequest/` is read, overrides and placeholders
   (`${uuid}`, `${randomNumber:3}`, `${ctx:placeId}` and so on) are applied, and the result is mapped to the request POJO.
2. **Call the endpoint.** `user calls "AddPlaceAPI" with "POST" http request` looks the endpoint up in
   `endpoints.yml`, adds the base URL and `key`, and sends it. If the connection cannot be opened, it is retried up to
   3 times.
3. **Carry `place_id` forward.** The `place_id` from Add is stored in the scenario context, so later Get, Update and
   Delete steps use it without being told.
4. **Validate.** Status code, response time and content type always; then, per step, fixtures from
   `placesSchema.json`, JSON Schemas from `jsonschema/`, and field-by-field read-back comparison.
5. **Clean up.** After every `@Places` scenario, places it created and did not delete are deleted, so the live
   server is not left with test data.
6. **Report.** Each request and response appears in the Extent Spark report (`reports/AutomationReport/`) and the
   Cucumber reports (`target/cucumber-reports/`). `Logs/automation.log` holds only failures: error responses (400+),
   calls that got no response, and failed scenarios with their last call. The expected 404s of the negative
   scenarios appear there too, as error responses.

## Running the Places tests

```powershell
mvn test                                                  # whole suite (all Places scenarios + unit tests)
mvn test "-Dcucumber.filter.tags=@Places"                 # Places scenarios only
mvn test "-Dcucumber.filter.tags=@AddPlace"               # one feature
mvn test "-Dcucumber.filter.tags=@Places and @smoke"      # smoke set
mvn test "-Dcucumber.filter.tags=@testcase=PLC-301"       # one scenario by ID
mvn test "-Dframework.http.log-level=ALL"                 # log every call, not only failures
```

`.\mvnw.cmd` works in place of `mvn`. In GitHub Actions, open **Actions → API Tests CI → Run workflow** and enter
the same tag expression.

## Live API vs. documentation

The live Get Place response differs from the API document. The suite accepts both forms and still compares every
value:

| Field | Documented | Returned live |
|---|---|---|
| `location` | `lat` / `lng` | `latitude` / `longitude` |
| Numbers (`accuracy`, coordinates) | numbers | strings |
| `types` | list | comma-joined string |

## Adding a new Places test

1. **Scenario:** add it to the matching `.feature` file with a new `@testcase=PLC-xxx` ID and `@smoke`,
   `@regression` or `@negative`. Reuse existing steps where possible.
2. **New body variant:** add a JSON template to `requestjsonfiles/PlacesRequest/`, or a record to
   `testdata/places/placesTestData.yml`.
3. **New expected response:** add a named entry to `schemas/PlacesSchema/placesSchema.json`; it can use matcher
   tokens such as `<notEmpty>` or `<regex:...>`.
4. **New step:** put it in the step class for that endpoint, and keep the HTTP logic in `PlacesRequestService` and
   the assertions in `PlacesResponseValidator`.
5. **New Places endpoint:** add it under `places` in `endpoints.yml`, add its name constant to
   `PlacesRequestService`, and add request and response POJOs.
