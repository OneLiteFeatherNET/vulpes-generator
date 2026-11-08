package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.client.GithubService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The legacy source download, the build data and the generation stub are removed; they answer 404. The branch list
 * stays for the editor release flow. GitHub is replaced by a mock, so no test makes a network call.
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

    @Singleton
    @Replaces(GithubService.class)
    GithubService mockGithubService() {
        return Mockito.mock(GithubService.class);
    }

    @Test
    void legacySourceDownloadAnswers404() {
        assertEquals(404, statusOf("/download"), "GET /download was removed and must answer 404");
    }

    @Test
    void legacyBuildDataAnswers404() {
        assertEquals(404, statusOf("/build/data"), "GET /build/data was removed and must answer 404");
    }

    @Test
    void legacyGenerationStubAnswers404() {
        assertEquals(404, statusOf("/generator/generate"), "GET /generator/generate was removed and must answer 404");
    }

    @Test
    void branchListIsStillServed() {
        assertNotEquals(404, statusOf("/git/branches"), "GET /git/branches stays for the editor release flow");
    }

    private int statusOf(String path) {
        try {
            return client.toBlocking().exchange(HttpRequest.GET(path)).getStatus().getCode();
        } catch (HttpClientResponseException e) {
            return e.getStatus().getCode();
        }
    }
}
