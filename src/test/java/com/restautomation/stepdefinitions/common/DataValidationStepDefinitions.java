package com.restautomation.stepdefinitions.common;

import com.restautomation.commons.utilities.common.PlaceholderResolver;
import com.restautomation.commons.utilities.datavalidation.ComparisonOptions;
import com.restautomation.commons.utilities.datavalidation.DataSet;
import com.restautomation.commons.utilities.datavalidation.DataValidationService;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.context.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic data-validation steps: API response ↔ database ↔ external files (CSV, Excel, JSON, YAML).
 * Not used by the Places suite; kept for future DB validation.
 * <p>
 * {@code <api>} is a DataType name or apiName (queries.yml key). Query parameters are comma separated and
 * may use placeholders such as {@code ${ctx:key}}. Append "with opt-out filter" to apply the configured
 * {@code {{OPT_OUT_FILTER}}} condition.
 */
@RequiredArgsConstructor
public class DataValidationStepDefinitions {

    private final DataValidationService dataValidationService;
    private final PlaceholderResolver placeholderResolver;
    private final ScenarioContext context;

    @Then("response list {string} should match records in file {string} using key columns {string}")
    public void responseListShouldMatchFile(String jsonPath, String file, String keys) {
        ComparisonOptions options = byKeys(keys);
        dataValidationService.assertMatch(
                dataValidationService.compareResponseWithFile(context.getResponse(), jsonPath, file, null, options), options);
    }

    @Then("response list {string} should match records in sheet {string} of file {string} using key columns {string}")
    public void responseListShouldMatchSheet(String jsonPath, String sheet, String file, String keys) {
        ComparisonOptions options = byKeys(keys);
        dataValidationService.assertMatch(
                dataValidationService.compareResponseWithFile(context.getResponse(), jsonPath, file, sheet, options), options);
    }

    @Then("response list {string} should match database query {string} for {word} API with parameters {string} using key columns {string}")
    public void responseListShouldMatchDatabase(String jsonPath, String variant, String api, String params, String keys) {
        compareResponseWithDatabase(jsonPath, variant, api, params, keys, false);
    }

    @Then("response list {string} should match database query {string} for {word} API with parameters {string} with opt-out filter using key columns {string}")
    public void responseListShouldMatchDatabaseWithOptOut(String jsonPath, String variant, String api, String params, String keys) {
        compareResponseWithDatabase(jsonPath, variant, api, params, keys, true);
    }

    @Then("database query {string} for {word} API with parameters {string} should match records in file {string} using key columns {string}")
    public void databaseQueryShouldMatchFile(String variant, String api, String params, String file, String keys) {
        ComparisonOptions options = byKeys(keys);
        dataValidationService.assertMatch(dataValidationService.compareDatabaseWithFile(
                DataType.fromName(api), variant, params(params), false, file, null, options), options);
    }

    /** Inline expectations; only the table's columns are compared. */
    @Then("response list {string} should contain exactly these records:")
    public void responseListShouldContainRecords(String jsonPath, DataTable table) {
        List<Map<String, Object>> rows = new ArrayList<>();
        table.asMaps().forEach(row -> rows.add(new LinkedHashMap<>(row)));
        ComparisonOptions options = ComparisonOptions.builder().includeColumns(table.row(0)).build();
        DataSet expected = DataSet.fromRows("feature-file table", rows);
        dataValidationService.assertMatch(dataValidationService.compare(
                dataValidationService.fromResponse(context.getResponse(), jsonPath), expected, options), options);
    }

    private void compareResponseWithDatabase(String jsonPath, String variant, String api, String params, String keys, boolean optOut) {
        ComparisonOptions options = byKeys(keys);
        dataValidationService.assertMatch(dataValidationService.compareResponseWithDatabase(
                context.getResponse(), jsonPath, DataType.fromName(api), variant, params(params), optOut, options), options);
    }

    private List<String> params(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(placeholderResolver.resolve(raw).split(",")).map(String::trim).toList();
    }

    private static ComparisonOptions byKeys(String keys) {
        return ComparisonOptions.byKeys(Arrays.stream(keys.split(",")).map(String::trim).toArray(String[]::new));
    }
}
