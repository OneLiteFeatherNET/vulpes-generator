package net.onelitefeather.vulpes.generator.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The editor route with {@code vulpes.release.editor-route-enabled} at its default: the route does not exist.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditorReleaseDisabledIntegrationTest implements TestPropertyProvider {

    private static final String BODY = "{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}";

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

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        repository.reset();
    }

    @Test
    void editorRouteAnswers404WhenDisabled() {
        assertEquals(404, status(), "POST /generator/release must answer 404 when the editor route is disabled");
    }

    @Test
    void nothingIsFetchedOrUploadedWhenDisabled() {
        status();

        assertEquals(0, backend.requestCount(), "a disabled route must not contact the backend");
        assertTrue(repository.keys().isEmpty(), "a disabled route must not upload anything");
    }

    private int status() {
        try {
            return client.toBlocking().exchange(
                    HttpRequest.POST("/generator/release", BODY).contentType(MediaType.APPLICATION_JSON_TYPE)).getStatus().getCode();
        } catch (HttpClientResponseException e) {
            return e.getStatus().getCode();
        }
    }
}
