package com.restautomation.commons.utilities.requesthelpers;

import com.restautomation.commons.utilities.common.MaskingUtils;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.context.HttpExchange;
import com.restautomation.context.ScenarioContext;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logs every request/response with secrets masked, measures response time and keeps the last
 * exchange on the {@link ScenarioContext} so failed scenarios can attach it to the report.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAssuredFilter implements Filter {

    private final ReadConfigYaml config;
    private final ScenarioContext scenarioContext;

    @Override
    public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification responseSpec,
                           FilterContext context) {
        long start = System.nanoTime();
        Response response = context.next(request, responseSpec);
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        String requestLog = describeRequest(request);
        String responseLog = describeResponse(response, elapsedMillis);
        String level = config.getHttp().getLogLevel();
        boolean failure = response.statusCode() >= 400;
        if ("ALL".equalsIgnoreCase(level) || ("FAILURES".equalsIgnoreCase(level) && failure)) {
            log.info("\n{}\n{}", requestLog, responseLog);
        } else {
            log.debug("\n{}\n{}", requestLog, responseLog);
        }
        Object requestBody = request.getBody();
        HttpExchange exchange = new HttpExchange(
                request.getMethod(),
                request.getURI(),
                request.getContentType(),
                requestBody == null ? null : MaskingUtils.maskJsonSecrets(String.valueOf(requestBody)),
                response.statusCode(),
                response.getContentType(),
                MaskingUtils.maskJsonSecrets(response.asString()),
                elapsedMillis);
        record(requestLog, responseLog, exchange);
        return response;
    }

    private void record(String requestLog, String responseLog, HttpExchange exchange) {
        try {
            scenarioContext.setLastRequestLog(requestLog);
            scenarioContext.setLastResponseLog(responseLog);
            scenarioContext.setLastResponseTimeMillis(exchange.timeMillis());
            scenarioContext.setLastExchange(exchange);
        } catch (RuntimeException outsideScenario) {
            // Called outside a running scenario (e.g. warm-up); nothing to record.
        }
    }

    private String describeRequest(FilterableRequestSpecification request) {
        List<String> masked = config.getHttp().getMaskedHeaders();
        String headers = request.getHeaders().asList().stream()
                .map(h -> "  " + h.getName() + ": " + MaskingUtils.maskIfSensitive(h.getName(), h.getValue(), masked))
                .collect(Collectors.joining("\n"));
        StringBuilder text = new StringBuilder("REQUEST  ").append(request.getMethod()).append(' ').append(request.getURI());
        if (!headers.isEmpty()) {
            text.append('\n').append(headers);
        }
        if (!request.getFormParams().isEmpty()) {
            text.append("\n  form: ").append(request.getFormParams().entrySet().stream()
                    .map(e -> e.getKey() + "=" + MaskingUtils.maskIfSensitive(e.getKey(), e.getValue(), masked))
                    .collect(Collectors.joining("&")));
        }
        Object body = request.getBody();
        if (body != null) {
            text.append("\n  body: ").append(truncate(MaskingUtils.maskJsonSecrets(String.valueOf(body))));
        }
        return text.toString();
    }

    private String describeResponse(Response response, long elapsedMillis) {
        return "RESPONSE " + response.statusCode() + " (" + elapsedMillis + " ms)\n  body: "
                + truncate(MaskingUtils.maskJsonSecrets(response.asString()));
    }

    private String truncate(String text) {
        int max = config.getHttp().getMaxLogBodyChars();
        return text != null && text.length() > max ? text.substring(0, max) + "... [truncated]" : text;
    }
}
