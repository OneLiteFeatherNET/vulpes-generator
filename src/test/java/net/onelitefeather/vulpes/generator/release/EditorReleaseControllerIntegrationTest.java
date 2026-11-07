package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.digest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@code POST /generator/release} with the editor route enabled. The request carries no bearer token: the
 * route is not behind the release token.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditorReleaseControllerIntegrationTest implements TestPropertyProvider {

    private static final String RELEASES = "onelitefeather-releases/net/onelitefeather/vulpes/content/eldoria/1.2.0/";
    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort(Map.of("vulpes.release.editor-route-enabled", "true"));
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    FakeBackend backend;

    @Inject
    FakeMavenRepository repository;

    @Inject
    BackendApiReleaseSnapshotSource source;

    @Inject
    FakeBaseBranches branches;

    @Inject
    FakeBaseVerification verification;

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        repository.reset();
        branches.reset();
        verification.reset();
    }

    @Test
    void editorReleaseAnswers201WithCoordinatesAndTheRevisionOfTheAssembledSnapshot() {
        String revision = digest(source.fetch(PROJECT_ID));

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(201, outcome.status(), "an editor release of a known project must answer 201");
        JsonNode body = json(outcome.body());
        assertEquals("net.onelitefeather.vulpes.content:eldoria:1.2.0", body.get("coordinates").asText(),
                "the body must carry the project key as coordinates");
        assertEquals(revision, body.get("revision").asText(), "the body must carry the revision of the snapshot");
        assertEquals("develop", body.get("base").get("branch").asText(), "an omitted baseBranch must be develop");
        assertEquals(ReleaseTestData.BASE_COMMIT, body.get("base").get("commit").asText(),
                "base.commit must be the full commit that was verified");
        assertTrue(body.get("repositoryUrl").asText().endsWith("/onelitefeather-releases"),
                "the body must name the repository of the version: " + outcome.body());
    }

    @Test
    void editorReleaseOfAnUnknownBaseBranchAnswers422AndUploadsNothing() {
        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\",\"baseBranch\":\"no-such-branch\"}");

        assertEquals(422, outcome.status(), "an unknown base branch must answer 422");
        assertTrue(outcome.body().contains("UNKNOWN_BASE_BRANCH"), "the body must name the reason: " + outcome.body());
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for an unknown base branch");
    }

    @Test
    void editorReleaseAgainstAFeatureBranchRecordsTheBranch() {
        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\",\"baseBranch\":\"feature/font\"}");

        assertEquals(201, outcome.status(), "a known feature branch must answer 201");
        assertEquals("feature/font", json(outcome.body()).get("base").get("branch").asText(),
                "base.branch must be the requested branch");
    }

    @Test
    void invalidVersionsAnswer400BeforeTheBackendIsCalled() {
        Outcome leadingZero = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.02.0\"}");
        Outcome retiredDev = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0-dev.1\"}");

        assertEquals(400, leadingZero.status(), "1.02.0 must answer 400");
        assertEquals(400, retiredDev.status(), "1.2.0-dev.1 must answer 400");
        assertEquals(0, backend.requestCount(), "an invalid version must not reach the backend");
    }

    @Test
    void snapshotVersionThatAlreadyExistsIsReplacedAnswers201() {
        repository.store("onelitefeather-snapshots/net/onelitefeather/vulpes/content/eldoria/1.2.0-SNAPSHOT/eldoria-1.2.0-SNAPSHOT.pom",
                new byte[]{1});

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0-SNAPSHOT\"}");

        assertEquals(201, outcome.status(), "an existing snapshot version must be replaced, not refused");
    }

    @Test
    void editorReleaseNeedsNoReleaseToken() {
        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(201, outcome.status(), "the editor route must not require the release token");
        assertTrue(repository.contains(RELEASES + "eldoria-1.2.0.jar"), "the JAR must be uploaded without a token");
    }

    @Test
    void editorReleaseIsCreatedByStelarisAtTheInjectedClockTime() throws IOException {
        post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        JsonNode provenance = releaseJson(repository.get(RELEASES + "eldoria-1.2.0.jar")).get("provenance");

        assertEquals("stelaris", provenance.get("createdBy").asText(), "the creator must be stelaris");
        assertEquals("2026-10-28T12:00:00Z", provenance.get("createdAt").asText(),
                "createdAt must be the time of the injected clock");
    }

    @Test
    void existingVersionAnswers409AndUploadsNothingNew() {
        repository.store(RELEASES + "eldoria-1.2.0.pom", new byte[]{1});

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(409, outcome.status(), "an existing version must answer 409");
        assertEquals(List.of(RELEASES + "eldoria-1.2.0.pom"), List.copyOf(repository.keys()),
                "nothing may be uploaded for an existing version");
    }

    @Test
    void projectThatKeepsChangingAnswers409AndUploadsNothing() {
        AtomicInteger reads = new AtomicInteger();
        backend.putObject("project/" + PROJECT_ID, () -> FakeBackend.project(PROJECT_ID, "eldoria", "Eldoria " + reads.incrementAndGet()));

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(409, outcome.status(), "a project that keeps changing while it is read must answer 409");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for a project that kept changing");
    }

    @Test
    void unknownProjectAnswers422AndUploadsNothing() {
        Outcome outcome = post("{\"projectId\":\"" + UNKNOWN_PROJECT + "\",\"version\":\"1.2.0\"}");

        assertEquals(422, outcome.status(), "a project unknown to the backend must answer 422");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for an unknown project");
    }

    @Test
    void projectWithUnsupportedContentAnswers422NamingIt() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        fixture.advancements().add(obj("id", id(10), "key", "first_page"));
        backend.registerProject(fixture.build());

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(422, outcome.status(), "a project with an advancement must answer 422");
        assertTrue(outcome.body().contains("first_page"), "the body must name the advancement, was: " + outcome.body());
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for unsupported content");
    }

    @Test
    void unavailableBackendAnswers502AndUploadsNothing() {
        backend.setDown(true);

        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.2.0\"}");

        assertEquals(502, outcome.status(), "an unavailable backend must answer 502");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded when the backend is unavailable");
    }

    @Test
    void invalidVersionAnswers400AndUploadsNothing() {
        Outcome outcome = post("{\"projectId\":\"" + PROJECT_ID + "\",\"version\":\"1.02.0\"}");

        assertEquals(400, outcome.status(), "a version with a leading zero must answer 400, as on POST /releases");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for an invalid version");
    }

    private static JsonNode json(String body) {
        try {
            return ProjectFixture.JSON.readTree(body);
        } catch (Exception e) {
            throw new AssertionError("the body is not JSON: " + body, e);
        }
    }

    private Outcome post(String json) {
        try {
            HttpResponse<String> response = client.toBlocking().exchange(
                    HttpRequest.POST("/generator/release", json).contentType(MediaType.APPLICATION_JSON_TYPE), String.class);
            return new Outcome(response.getStatus().getCode(), response.body());
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private static JsonNode releaseJson(byte[] jar) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(DataJarBuilder.RELEASE_JSON_NAME)) {
                    return ProjectFixture.JSON.readTree(zip.readAllBytes());
                }
            }
        }
        throw new AssertionError("the JAR has no " + DataJarBuilder.RELEASE_JSON_NAME);
    }

    private record Outcome(int status, String body) {
    }
}
