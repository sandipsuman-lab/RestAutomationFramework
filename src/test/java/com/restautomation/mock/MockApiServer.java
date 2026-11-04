package com.restautomation.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Local stand-in for every HTTP dependency (token service, APIs under test, tenant metadata API,
 * legacy XML service), driven by the mappings under {@code src/test/resources/wiremock}.
 * Active only when {@code mock.server.enabled=true} (the {@code local} profile).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mock.server", name = "enabled", havingValue = "true")
public class MockApiServer {

    @Value("${mock.server.port:8089}")
    private int port;

    private WireMockServer server;

    @PostConstruct
    public void start() {
        server = new WireMockServer(WireMockConfiguration.options()
                .port(port)
                .usingFilesUnderClasspath("wiremock")
                .extensions(new PlacesMockExtension()));
        server.start();
        log.info("Mock API server started on http://localhost:{} with {} stubs", port, server.getStubMappings().size());
    }

    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}
