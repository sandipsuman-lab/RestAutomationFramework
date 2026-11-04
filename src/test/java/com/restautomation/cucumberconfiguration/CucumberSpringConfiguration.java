package com.restautomation.cucumberconfiguration;

import com.restautomation.SpringBootRunner;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/** One Spring context shared by every step class; framework beans are injected into steps. */
@CucumberContextConfiguration
@SpringBootTest(classes = SpringBootRunner.class)
public class CucumberSpringConfiguration {
}
