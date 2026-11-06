# REST API Automation Framework

Cucumber + Spring Boot + TestNG + REST Assured tests for the **Places API** (Add / Get / Update / Delete Place),
with a **database / file data-validation layer** kept ready for future use. See [ARCHITECTURE.md](ARCHITECTURE.md)
for the original design this framework follows.

## Quick start

```bash
./mvnw clean test                                                  # Places suite (live API) + unit tests
./mvnw clean test -Dcucumber.filter.tags="@AddPlace and @smoke"    # one feature / tag
./mvnw clean verify -Dthreads=4 -Dmaven.test.failure.ignore=true   # parallel + masterthought HTML report
```

Requires JDK 17+ (verified on 17 and 26) and internet access for the live Places API
(`https://rahulshettyacademy.com`, key `qaclick123`).

| Override | Example |
|---|---|
| Tags (default `not @wip`) | `-Dcucumber.filter.tags="@UpdatePlace"` |
| Profile (`local` default, `stage` = Snowflake) | `-Dspring.profiles.active=stage` |
| Parallel threads | `-Dthreads=4` |
| Any framework setting | `-Dframework.http.max-response-time-millis=10000` |

## CI/CD (GitHub Actions)

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs the suite against the live Places API on GitHub-hosted
Ubuntu runners (JDK 17):

| Trigger | When |
|---|---|
| `push` / `pull_request` | to `main`, `master` or `develop` |
| `schedule` | daily at 02:30 UTC |
| `workflow_dispatch` | Actions tab → *API Tests CI* → *Run workflow*, with optional `tags` and `threads` |

Each run writes a pass/fail table to the run summary, uploads the Extent Spark, Cucumber, masterthought and TestNG
reports plus logs as the `api-test-reports-<run>` artifact (kept 14 days), and fails the job if any test fails or
no tests ran. [`automation-testing.yaml`](.github/workflows/automation-testing.yaml) is a separate reusable workflow
for calling this suite from other repositories.

## Places API suite

| Feature (`src/test/resources/features/PlacesFeature/`) | Covers |
|---|---|
| `AddPlace.feature` | default payload; outline over names/addresses/languages; data-driven from `testdata/places/placesTestData.yml` |
| `GetPlace.feature` | read-back of every field; 404 for unknown / invalid place_id |
| `UpdatePlace.feature` | address update visible on read-back; other fields unchanged; 404 for unknown place |
| `DeletePlace.feature` | deleted place returns 404; double delete and unknown place rejected |
| `PlaceLifecycle_E2E.feature` | Add → Get → Update → Get → Delete → Get |

- **Endpoints** are in `src/test/resources/endpoints.yml`: base URL, the `key` query param, response content type,
  and method + resource for `AddPlaceAPI`, `GetPlaceAPI`, `UpdatePlaceAPI`, `DeletePlaceAPI`. Base URL and key come
  from `places.*` in `application.yml`. Steps call endpoints by name: `user calls "AddPlaceAPI" with "POST" http request`,
  or `user calls "AddPlaceAPI" endpoint` to use the YAML method.
- **Specifications** (`SpecificationFactory`): a base `RequestSpecBuilder` spec (timeouts, Jackson mapper, SSL, logging
  filter), a per-API spec from endpoints.yml (base URI, key, headers), and a `ResponseSpecBuilder` spec used by
  `user should get response code N` (status, `framework.http.max-response-time-millis`, content type for 2xx).
- **POJOs**: requests `models/requestmodels/places/` (`AddPlaceRequest`, `Location`, `UpdatePlaceRequest`,
  `DeletePlaceRequest`), responses `models/responsefromjson/places/` (`AddPlaceResponse`, `GetPlaceResponse`,
  `PlaceMessageResponse`). Add Place bodies come from `requestjsonfiles/PlacesRequest/addPlace.json` plus overrides.
- **Steps**: `stepdefinitions/placesSteps/` (`PlacesCommonStepDefinitions` + one class per feature), backed by
  `services/request/PlacesRequestService` and `responsevalidator/places/PlacesResponseValidator`.
  The place_id from Add is reused automatically, and every place a scenario creates is deleted afterwards.
- **Checks**: response spec, expected-response fixtures (`schemas/PlacesSchema/placesSchema.json`), JSON Schemas
  (`jsonschema/`), field-by-field read-back comparison, response time and headers.
- **Reports**: each call's request and response payloads appear in the calling step of the Extent Spark report
  (`reports/AutomationReport/`) and as Cucumber attachments (`target/cucumber-reports/`).
- **Logs** (`Logs/automation.log`): every call goes to production, and only failures are logged — error responses
  (400+), calls that got no response, and failed scenarios with their last call — plus one result line per scenario.
  Use `-Dframework.http.log-level=ALL` to log every call for one run.

The live Get Place response differs from the API document: `location` comes back as `latitude`/`longitude`
(documented `lat`/`lng`), numbers as strings, and `types` as a comma-joined string. The suite accepts these forms
and still compares every value.

## Database layer (kept for future use)

The Places suite does not validate against a database. These pieces are kept ready to use:

| What | Where |
|---|---|
| JDBC execution (Snowflake / H2), row normalisation, `{{OPT_OUT_FILTER}}` | `commons/utilities/dbhelpers/SnowFlakeDbConnector` |
| SQL per API and variant | `src/test/resources/queries.yml` → `commons/db/ReadDbQueryFromYAML` |
| Dispatch by `DataType` / query variant | `commons/utilities/parsers/DbQueryParser`, `enumhelpers/EndPoints` |
| Example controller / row mapper / row POJO | `controller/db/ItemsController`, `services/db/ItemsUtilityService`, `models/responsefromdb/getItems/ItemsDbRecord` (`DataType.GetItems`) |
| Comparison engine (API / DB / CSV / Excel / JSON / YAML) | `commons/utilities/datavalidation/*`, readers in `commons/utilities/readerhelpers/` |
| Data-validation steps | `stepdefinitions/common/DataValidationStepDefinitions` |
| Local H2 warehouse (profile `local`) | `src/test/resources/db/schema-local.sql`, `data-local.sql` |
| Snowflake connection (profile `stage`) | `spring.datasource.*` in `application.yml`; `SNOWFLAKE_*` env vars |

Available steps:

```gherkin
Then response list "$.items" should match database query "byDateRange" for GetItems API with parameters "T100,D10,2026-10-01 00:00:00,2026-10-31 23:59:59" using key columns "itemId"
Then response list "$.items" should match database query "byTenant" for GetItems API with parameters "T100,D10" with opt-out filter using key columns "itemId"
Then database query "byTenant" for GetItems API with parameters "T100,D10" should match records in file "items/expected.yml" using key columns "itemId"
Then response list "$.items" should match records in sheet "Items" of file "items/expected.xlsx" using key columns "itemId"
```

The comparison matches columns across naming styles (`itemId` = `item_id` = `Item Id`) and values across types
(`"25.5"` = `25.50`, `"Y"` = `true`, `2026-10-05 10:15:30` = `2026-10-05T10:15:30Z`), reports every mismatch, and
writes a CSV to `target/data-validation/` on failure.

To add DB validation for an API: add its SQL to `queries.yml`, a `DataType` constant, a `BaseDbController` +
`AbstractTransformerService` + row POJO, then call `DbQueryParser` / `DataValidationService` from a validator or use
the steps above.

## Reusable utilities

| Package | Classes |
|---|---|
| `commons/utilities/common` | `JsonUtils`, `JsonComparator` (fixture matcher tokens), `DateTimeUtils`, `RandomDataUtils`, `PlaceholderResolver`, `MaskingUtils`, `FileUtils`, `UrlUtils` |
| `commons/utilities/readerhelpers` | `ReadJsonFile`, `YamlResourceReader`, `CsvFileReader`, `ExcelFileReader`, `TestDataReader` |
| `commons/utilities/requesthelpers` | `RequestGenerator`, `RequestSetup`, `SpecificationFactory`, `RestAssuredFilter`, `RestAssuredConfigFactory` |
| `commons/utilities/parsers` | `ResponseParser`, `ResponseToClassObjectParser`, `RequestToClassObjectParser`, `RequestPathParser`, `SchemaValidatorParser`, `DataConversionHelper`, `DbQueryParser`, `ParseTableNames` |

**Placeholders** in templates, step values and SQL parameters: `${uuid}`, `${randomString:8}`, `${randomNumber:6}`,
`${randomEmail}`, `${now}`, `${now-4d:yyyy-MM-dd}`, `${ctx:key}`, `${env:VAR}`, `${prop:key}`.
Template overrides use JsonPath keys plus `<remove>` / `<null>`.

**Fixture matcher tokens** in `schemas/**`: `<any>`, `<notNull>`, `<notEmpty>`, `<number>`, `<boolean>`,
`<isoDateTime>`, `<uuid>`, `<regex:...>`, `<oneOf:a|b>`.
