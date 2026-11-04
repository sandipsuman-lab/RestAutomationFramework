package com.restautomation.hooks;

import com.restautomation.context.ScenarioContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * Scenario lifecycle:
 * <ol>
 *   <li>before – bind the scenario to the context and to the log MDC ({@code %X{scenario}})</li>
 *   <li>after (order 100, i.e. before clean-up hooks with lower order) – on failure, attach the last
 *       request/response to the report</li>
 * </ol>
 */
@Slf4j
@RequiredArgsConstructor
public class Hooks {

    private final ScenarioContext context;

    @Before(order = 0)
    public void initialiseScenario(Scenario scenario) {
        MDC.put("scenario", scenario.getName());
        context.setScenario(scenario);
        log.info(">>> START  {} {}", scenario.getName(), scenario.getSourceTagNames());
    }

    @After(order = 100)
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            attach(scenario, "Last request", context.getLastRequestLog());
            attach(scenario, "Last response", context.getLastResponseLog());
        }
        log.info("<<< {} {}", scenario.getStatus(), scenario.getName());
        MDC.remove("scenario");
    }

    private static void attach(Scenario scenario, String name, String text) {
        if (text != null) {
            scenario.attach(text, "text/plain", name);
        }
    }
}
