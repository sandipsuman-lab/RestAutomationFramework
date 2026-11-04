package com.restautomation.commons.utilities.requesthelpers;

import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.config.ReadConfigYaml;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.stereotype.Component;

import java.net.ConnectException;

/**
 * Builds a per-request {@link RestAssuredConfig} (timeouts, shared Jackson mapper, SSL) instead of
 * mutating REST Assured's static globals, which keeps scenarios safe to run in parallel.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAssuredConfigFactory {

    private final ReadConfigYaml config;

    public RestAssuredConfig create() {
        ReadConfigYaml.Http http = config.getHttp();
        int connectRetries = Math.max(0, http.getConnectRetries());
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .httpClientFactory(() -> clientWithConnectRetries(connectRetries))
                        .setParam("http.connection.timeout", http.getConnectTimeoutMillis())
                        .setParam("http.socket.timeout", http.getReadTimeoutMillis()))
                .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                        .jackson2ObjectMapperFactory((type, charset) -> JsonUtils.mapper()))
                .encoderConfig(EncoderConfig.encoderConfig()
                        .appendDefaultContentCharsetToContentTypeIfUndefined(false))
                .sslConfig(http.isRelaxedHttps() ? SSLConfig.sslConfig().relaxedHTTPSValidation() : SSLConfig.sslConfig());
    }

    /**
     * HTTP client that retries only when the TCP connection cannot be opened (refused or timed out).
     * Nothing has been sent at that point, so the retry is safe for every method, POST and DELETE included.
     * Failures after the connection is open are never retried.
     */
    @SuppressWarnings("deprecation")
    private static org.apache.http.client.HttpClient clientWithConnectRetries(int connectRetries) {
        DefaultHttpClient client = new DefaultHttpClient();
        client.setHttpRequestRetryHandler((exception, executionCount, context) -> {
            boolean connectFailure = exception instanceof ConnectException
                    || exception instanceof ConnectTimeoutException;
            if (connectFailure && executionCount <= connectRetries) {
                log.warn("Connection attempt {} failed ({}); retrying ({} of {})",
                        executionCount, exception.getMessage(), executionCount, connectRetries);
                return true;
            }
            return false;
        });
        return client;
    }
}
