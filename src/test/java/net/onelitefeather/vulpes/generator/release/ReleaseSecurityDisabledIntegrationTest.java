package net.onelitefeather.vulpes.generator.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fail-closed behaviour: with a blank {@code vulpes.release.token} every release request is refused,
 * whatever it carries, while the operational endpoints stay available.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReleaseSecurityDisabledIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>(ReleaseTestProperties.onFreePort());
        properties.put("vulpes.release.token", "");
        return properties;
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void requestWithoutAuthorizationIsRefusedWhenNoTokenIsConfigured() {
        assertEquals(401, post(null), "without a configured token a request without Authorization must answer 401");
    }

    @Test
    void requestWithAnyTokenIsRefusedWhenNoTokenIsConfigured() {
        assertEquals(401, post("Bearer anything"), "without a configured token every bearer token must answer 401");
    }

    @Test
    void requestWithAnEmptyBearerTokenIsRefusedWhenNoTokenIsConfigured() {
        assertEquals(401, post("Bearer "), "an empty bearer token must never match a blank configured token");
    }

    @Test
    void healthStaysAvailableWhenReleasesAreDisabled() {
        assertEquals(200, status(HttpRequest.GET("/health")), "/health must stay available without a release token");
    }

    private int post(String authorization) {
        MutableHttpRequest<String> request = HttpRequest.POST("/releases", "{}").contentType(MediaType.APPLICATION_JSON_TYPE);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return status(request);
    }

    private int status(HttpRequest<?> request) {
        try {
            HttpResponse<?> response = client.toBlocking().exchange(request);
            return response.getStatus().getCode();
        } catch (HttpClientResponseException e) {
            return e.getStatus().getCode();
        }
    }
}
