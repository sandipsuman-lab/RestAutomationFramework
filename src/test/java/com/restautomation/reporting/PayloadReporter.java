package com.restautomation.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.context.HttpExchange;
import com.restautomation.context.ScenarioContext;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes an HTTP exchange into the current step of the Extent Spark report (a status label plus the
 * request and response bodies as formatted JSON) and attaches it to the Cucumber report. Reporting
 * problems are logged and never fail a scenario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayloadReporter {

    private final ScenarioContext context;

    /** Reports the last HTTP call of the current scenario under {@code title} (e.g. the endpoint name). */
    public void reportLastExchange(String title) {
        HttpExchange exchange = context.getLastExchange();
        if (exchange == null) {
            return;
        }
        try {
            toSpark(title, exchange);
            toCucumber(title, exchange);
        } catch (RuntimeException e) {
            log.warn("Could not add payload of {} to the report: {}", title, e.getMessage());
        }
    }

    private void toSpark(String title, HttpExchange exchange) {
        ExtentTest step = ExtentCucumberAdapter.getCurrentStep();
        if (step == null) {
            return; // Extent plugin not active for this run
        }
        ExtentColor color = exchange.statusCode() < 400 ? ExtentColor.GREEN : ExtentColor.ORANGE;
        step.info(MarkupHelper.createLabel(title + " | " + exchange.method() + " " + exchange.uri()
                + " | HTTP " + exchange.statusCode() + " | " + exchange.timeMillis() + " ms", color));
        if (exchange.requestBody() != null && !exchange.requestBody().isBlank()) {
            step.info("Request payload");
            step.info(codeBlock(exchange.requestBody()));
        }
        step.info("Response payload");
        step.info(codeBlock(exchange.responseBody()));
    }

    private void toCucumber(String title, HttpExchange exchange) {
        Scenario scenario = context.getScenario();
        if (scenario == null) {
            return;
        }
        if (exchange.requestBody() != null && !exchange.requestBody().isBlank()) {
            scenario.attach(pretty(exchange.requestBody()), mediaType(exchange.requestBody()), title + " request");
        }
        scenario.attach(pretty(exchange.responseBody()), mediaType(exchange.responseBody()),
                title + " response (HTTP " + exchange.statusCode() + ")");
    }

    private static com.aventstack.extentreports.markuputils.Markup codeBlock(String body) {
        if (body == null || body.isBlank()) {
            return MarkupHelper.createLabel("<empty body>", ExtentColor.GREY);
        }
        if (JsonUtils.isValidJson(body)) {
            return MarkupHelper.createCodeBlock(body, CodeLanguage.JSON);
        }
        return body.trim().startsWith("<") ? MarkupHelper.createCodeBlock(body, CodeLanguage.XML) : MarkupHelper.createCodeBlock(body);
    }

    private static String pretty(String body) {
        return body != null && JsonUtils.isValidJson(body) ? JsonUtils.toPrettyJson(body) : String.valueOf(body);
    }

    private static String mediaType(String body) {
        return body != null && JsonUtils.isValidJson(body) ? "application/json" : "text/plain";
    }
}
