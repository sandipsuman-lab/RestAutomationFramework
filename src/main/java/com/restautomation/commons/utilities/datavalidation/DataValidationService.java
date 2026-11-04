package com.restautomation.commons.utilities.datavalidation;

import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.parsers.DbQueryParser;
import com.restautomation.commons.utilities.parsers.ResponseParser;
import com.restautomation.commons.utilities.readerhelpers.TestDataReader;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.context.ScenarioContext;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * High-level data validation between the API under test, the warehouse and external files.
 * Every comparison produces a {@link ComparisonReport}; {@link #assertMatch} attaches it to the
 * Cucumber report, writes it to {@code target/data-validation/} and fails with a readable summary.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataValidationService {

    private final DataComparator comparator;
    private final TestDataReader testDataReader;
    private final DbQueryParser dbQueryParser;
    private final ScenarioContext context;
    private final ReadConfigYaml config;

    // ------------------------------------------------------------------ data set factories

    public DataSet fromResponse(Response response, String jsonPath) {
        return DataSet.fromJson("API response " + (jsonPath == null ? "" : jsonPath), ResponseParser.getBodyAsJson(response)
                .toString(), jsonPath);
    }

    public DataSet fromFile(String path, String selector) {
        String label = "file " + path + (selector == null || selector.isBlank() ? "" : " [" + selector + "]");
        return DataSet.fromRows(label, testDataReader.readRecords(path, selector));
    }

    public DataSet fromDatabase(DataType type, String queryVariant, List<String> params, boolean optOutFilter) {
        return DataSet.fromRows("DB " + type.getApiName() + "." + queryVariant + " " + params,
                dbQueryParser.executeNamedQuery(type, queryVariant, params, optOutFilter));
    }

    // ------------------------------------------------------------------ comparisons

    public ComparisonReport compare(DataSet actual, DataSet expected, ComparisonOptions options) {
        ComparisonReport report = comparator.compare(actual, expected, options);
        log.info("\n{}", report.summary(options.getMaxMismatchesInMessage()));
        return report;
    }

    public ComparisonReport compareResponseWithFile(Response response, String jsonPath, String filePath, String selector,
                                                    ComparisonOptions options) {
        return compare(fromResponse(response, jsonPath), fromFile(filePath, selector), options);
    }

    public ComparisonReport compareResponseWithDatabase(Response response, String jsonPath, DataType type, String queryVariant,
                                                        List<String> params, boolean optOutFilter, ComparisonOptions options) {
        return compare(fromResponse(response, jsonPath), fromDatabase(type, queryVariant, params, optOutFilter), options);
    }

    public ComparisonReport compareDatabaseWithFile(DataType type, String queryVariant, List<String> params, boolean optOutFilter,
                                                    String filePath, String selector, ComparisonOptions options) {
        return compare(fromDatabase(type, queryVariant, params, optOutFilter), fromFile(filePath, selector), options);
    }

    // ------------------------------------------------------------------ assertion

    public void assertMatch(ComparisonReport report, ComparisonOptions options) {
        String summary = report.summary(options.getMaxMismatchesInMessage());
        if (context.getScenario() != null) {
            context.getScenario().attach(report.summary(), "text/plain", "Data comparison");
        }
        if (!report.isMatch()) {
            String file = FileUtils.join(config.getPaths().getValidationReports(),
                    FileUtils.safeFileName(context.getScenarioName()) + "_"
                            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")) + ".csv");
            FileUtils.write(file, report.toCsv());
            throw new AssertionError(summary + "Full mismatch report: " + file);
        }
    }

    public void assertMatch(ComparisonReport report) {
        assertMatch(report, ComparisonOptions.defaults());
    }
}
