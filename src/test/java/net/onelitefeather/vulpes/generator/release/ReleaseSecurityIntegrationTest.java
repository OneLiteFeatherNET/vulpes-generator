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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.digest;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bearer token of {@code POST /releases} with a configured token. Nothing behind the filter may be
 * contacted when the token is missing or wrong.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReleaseSecurityIntegrationTest implements TestPropertyProvider {

    private static final String BODY = "{\"projectId\":\"" + PROJECT_ID + "\",\"project\":\"eldoria\",\"version\":\"1.2.0\","
            + "\"revision\":\"" + digest(snapshot(PROJECT_ID)) + "\",\"createdBy\":\"TheMeinerLP\","
            + "\"createdAt\":\"2026-10-28T12:00:00Z\"}";

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    FakeBackend backend;

    @Inject
    FakeMavenRepository repository;

    @Inject
    RecordingReleaseCompletionListener listener;

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        repository.reset();
        listener.reset();
        backend.register(PROJECT_ID, snapshot(PROJECT_ID));
    }

    @Test
    void requestWithoutAuthorizationHeaderAnswers401() {
        Outcome outcome = post(null);

        assertEquals(401, outcome.status(), "a request without Authorization must answer 401");
        assertEquals("Bearer", outcome.wwwAuthenticate(), "the 401 must challenge with the Bearer scheme");
        assertTrue(outcome.body().contains("\"reason\":\"UNAUTHORIZED\""),
                "the 401 body must name the reason, was: " + outcome.body());
    }

    @Test
    void requestWithoutAuthorizationContactsNothingBehindTheFilter() {
        post(null);

        assertEquals(0, backend.requestCount(), "the backend must not be contacted");
        assertTrue(repository.keys().isEmpty(), "the repository must not be touched");
        assertTrue(listener.completed().isEmpty(), "no release may complete");
    }

    @Test
    void wrongTokenAnswers401AndContactsNothing() {
        Outcome outcome = post("Bearer wrong-token");

        assertEquals(401, outcome.status(), "a wrong token must answer 401");
        assertEquals(0, backend.requestCount(), "the backend must not be contacted with a wrong token");
        assertTrue(repository.keys().isEmpty(), "the repository must not be touched with a wrong token");
    }

    @Test
    void basicSchemeWithTheTokenAnswers401() {
        Outcome outcome = post("Basic " + ReleaseTestProperties.TOKEN);

        assertEquals(401, outcome.status(), "a Basic Authorization header must answer 401, even with the token");
        assertEquals(0, backend.requestCount(), "the backend must not be contacted with the wrong scheme");
    }

    @Test
    void correctBearerTokenIsAccepted() {
        Outcome outcome = post("Bearer " + ReleaseTestProperties.TOKEN);

        assertEquals(201, outcome.status(), "the configured token must be accepted");
    }

    @Test
    void healthStaysAvailableWithoutToken() {
        Outcome outcome = get("/health");

        assertEquals(200, outcome.status(), "/health must stay available without the release token");
    }

    private Outcome post(String authorization) {
        MutableHttpRequest<String> request = HttpRequest.POST("/releases", BODY).contentType(MediaType.APPLICATION_JSON_TYPE);
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return send(request);
    }

    private Outcome get(String uri) {
        return send(HttpRequest.GET(uri));
    }

    private Outcome send(HttpRequest<?> request) {
        try {
            HttpResponse<String> response = client.toBlocking().exchange(request, String.class);
            return new Outcome(response.getStatus().getCode(), response.header("WWW-Authenticate"),
                    response.body() == null ? "" : response.body());
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), e.getResponse().header("WWW-Authenticate"),
                    e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private record Outcome(int status, String wwwAuthenticate, String body) {
    }
}
