package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
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
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GET /projects/{projectId}/validation} against the fake backend and the fake base. The check builds nothing,
 * so the base must never be verified by it.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContentValidationControllerIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

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
    FakeBaseVerification verification;

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        verification.reset();
    }

    @Test
    void validContentAnswers200WithNoProblems() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/validation");

        assertEquals(200, outcome.status(), "a known project must answer 200");
        assertEquals(0, outcome.problems().size(), "valid content must list no problem: " + outcome.body());
    }

    @Test
    void invalidContentAnswers200WithEveryProblemNamingItsEntry() {
        backend.registerProject(projectWithMissingSound());

        Outcome outcome = get("/projects/" + PROJECT_ID + "/validation");

        assertEquals(200, outcome.status(), "content with problems must still be answered with 200");
        assertEquals(2, outcome.problems().size(), "both missing sound files must be listed: " + outcome.body());
        assertTrue(outcome.problems().get(0).get("message").asText().startsWith("sound eldoria:vhs_static:"),
                "the problem must name the sound: " + outcome.body());
    }

    @Test
    void validationBuildsNothing() {
        backend.registerProject(projectWithMissingSound());

        get("/projects/" + PROJECT_ID + "/validation");

        assertTrue(verification.inputs().isEmpty(), "the validation must not verify any base");
    }

    @Test
    void baseBranchQueryParameterIsAcceptedAndDoesNotChangeTheAnswer() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/validation?baseBranch=feature/font");

        assertEquals(200, outcome.status(), "the query parameter must be accepted");
        assertTrue(verification.inputs().isEmpty(), "the query parameter must not start a base verification");
    }

    @Test
    void unknownProjectAnswers422() {
        assertEquals(422, get("/projects/" + UNKNOWN_PROJECT + "/validation").status(),
                "a project unknown to the backend must answer 422");
    }

    @Test
    void unavailableBackendAnswers502() {
        backend.setDown(true);

        assertEquals(502, get("/projects/" + PROJECT_ID + "/validation").status(),
                "an unavailable backend must answer 502");
    }

    /** The project of the default fixture with a sound that refers to two files without an asset. */
    private static ProjectData projectWithMissingSound() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        fixture.sounds().add(obj("id", ProjectFixture.id(5), "key", "vhs_static", "uiName", "vhs_static", "subTitle", null));
        fixture.sources(ProjectFixture.id(5)).add(obj("id", ProjectFixture.id(6), "name", "eldoria:vhs_static_1",
                "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false, "attenuationDistance", 16,
                "preload", false, "type", "FILE"));
        fixture.sources(ProjectFixture.id(5)).add(obj("id", ProjectFixture.id(7), "name", "eldoria:vhs_static_2",
                "volume", 1.0, "pitch", 1.0, "weight", 1, "stream", false, "attenuationDistance", 16,
                "preload", false, "type", "FILE"));
        return fixture.build();
    }

    private Outcome get(String uri) {
        try {
            HttpResponse<byte[]> response = client.toBlocking().exchange(HttpRequest.GET(uri), byte[].class);
            JsonNode body = ProjectFixture.JSON.readTree(response.body());
            return new Outcome(response.getStatus().getCode(), body.get("problems"), new String(response.body()));
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), null, "");
        } catch (Exception e) {
            throw new IllegalStateException("the validation answer is not JSON", e);
        }
    }

    private record Outcome(int status, JsonNode problems, String body) {
    }
}
