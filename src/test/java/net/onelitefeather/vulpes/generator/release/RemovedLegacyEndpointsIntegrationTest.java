package net.onelitefeather.vulpes.generator.release;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The legacy source download and the generation stub are removed; both routes answer 404.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemovedLegacyEndpointsIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void legacySourceDownloadAnswers404() {
        assertEquals(404, statusOf("/download"), "GET /download was removed and must answer 404");
    }

    @Test
    void legacyGenerationStubAnswers404() {
        assertEquals(404, statusOf("/generator/generate"), "GET /generator/generate was removed and must answer 404");
    }

    private int statusOf(String path) {
        try {
            return client.toBlocking().exchange(HttpRequest.GET(path)).getStatus().getCode();
        } catch (HttpClientResponseException e) {
            return e.getStatus().getCode();
        }
    }
}
