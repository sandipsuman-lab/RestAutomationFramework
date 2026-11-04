package com.restautomation.testrunner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * TestNG entry point. Scenarios run in parallel when {@code -Dthreads=N} (N > 1); scenario state is
 * scenario-scoped so this is safe. Tag selection: {@code -Dcucumber.filter.tags="@GetItems and @smoke"}
 * (default in cucumber.properties).
 */
@CucumberOptions(
        features = "classpath:features",
        glue = {
                "com.restautomation.stepdefinitions",
                "com.restautomation.cucumberconfiguration",
                "com.restautomation.hooks"
        },
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "rerun:target/cucumber-reports/rerun.txt",
                "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
        },
        monochrome = true
)
public class RunCucumberTest extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
