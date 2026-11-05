package com.restautomation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed view of {@code config.yml} (prefix {@code framework}).
 * <p>
 * {@code config.yml} holds environment-neutral defaults. Because it is loaded through
 * {@code @PropertySource} it has the lowest precedence, so any value can be overridden per
 * environment in {@code application.yml} profiles or with {@code -Dframework.x.y=...}.
 */
@Data
@Configuration(proxyBeanMethods = false)
@PropertySource(value = "classpath:config.yml", factory = YamlPropertySourceFactory.class)
@ConfigurationProperties(prefix = "framework")
public class ReadConfigYaml {

    private Paths paths = new Paths();
    private Http http = new Http();
    private Db db = new Db();
    private Validation validation = new Validation();

    @Data
    public static class Paths {
        private String schemas = "schemas";
        private String jsonSchemas = "jsonschema";
        private String requestFiles = "requestjsonfiles";
        private String testData = "testdata";
        private String validationReports = "target/data-validation";
    }

    @Data
    public static class Http {
        private int connectTimeoutMillis = 5_000;
        /** Extra attempts when the TCP connection cannot be opened (the request was never sent). */
        private int connectRetries = 3;
        private int readTimeoutMillis = 120_000;
        /** Upper bound enforced by every response specification. */
        private int maxResponseTimeMillis = 30_000;
        /** FAILURES (error responses and failed calls only), ALL or NONE. */
        private String logLevel = "FAILURES";
        private int maxLogBodyChars = 5_000;
        private boolean relaxedHttps = true;
        private List<String> maskedHeaders = new ArrayList<>(List.of("Authorization", "client_secret", "x-api-key"));
    }

    @Data
    public static class Db {
        /** Condition substituted for {{OPT_OUT_FILTER}} when opt-out filtering is requested. */
        private String optOutCondition = "OPT_OUT_FLAG = FALSE";
        /** Format of date-time values bound as query parameters. */
        private String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
        private int queryTimeoutSeconds = 300;
    }

    @Data
    public static class Validation {
        /** Fields that change on every call and are skipped when comparing payloads with fixtures. */
        private List<String> dynamicFields = new ArrayList<>(List.of("timestamp", "traceId", "correlationId"));
    }
}
