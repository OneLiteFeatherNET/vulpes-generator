package net.onelitefeather.vulpes.generator.release;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.id;
import static net.onelitefeather.vulpes.generator.release.ProjectFixture.obj;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.digest;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@code POST /releases} through the embedded server, with the fake backend and the fake Maven repository.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReleaseControllerIntegrationTest implements TestPropertyProvider {

    @Override
    public Map<String, String> getProperties() {
        return ReleaseTestProperties.onFreePort();
    }

    private static final String RELEASES = "onelitefeather-releases/net/onelitefeather/vulpes/content/eldoria/1.2.0/";
    private static final String SNAPSHOTS = "onelitefeather-snapshots/net/onelitefeather/vulpes/content/eldoria/1.2.0-SNAPSHOT/";
    /** Maven names a snapshot file with the version or, as Maven Resolver does on deploy, with a timestamp. */
    private static final Pattern SNAPSHOT_JAR = Pattern.compile("eldoria-1\\.2\\.0-(SNAPSHOT|\\d{8}\\.\\d{6}-\\d+)\\.jar$");
    private static final String ZERO_REVISION = "sha256:" + "0".repeat(64);
    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    FakeBackend backend;

    @Inject
    BackendApiReleaseSnapshotSource source;

    @Inject
    FakeMavenRepository repository;

    @Inject
    RecordingReleaseCompletionListener listener;

    @BeforeEach
    void resetEnvironment() {
        backend.reset();
        repository.reset();
        listener.reset();
    }

    @Test
    void successfulReleaseAnswers201WithCoordinatesAndRevision() {
        Outcome outcome = post(releaseJson("1.2.0", revision()));

        assertEquals(201, outcome.status(), "a published release must answer 201");
        assertEquals("{\"coordinates\":\"net.onelitefeather.vulpes.content:eldoria:1.2.0\",\"revision\":\"" + revision() + "\"}",
                outcome.body(), "the body must carry the coordinates and the revision");
    }

    @Test
    void successfulReleaseUploadsJarPomModuleAndAllChecksums() {
        post(releaseJson("1.2.0", revision()));

        for (String file : List.of("eldoria-1.2.0.jar", "eldoria-1.2.0.pom", "eldoria-1.2.0.module")) {
            assertTrue(repository.contains(RELEASES + file), "the repository must receive " + file);
            for (String algorithm : List.of("md5", "sha1", "sha256", "sha512")) {
                assertTrue(repository.contains(RELEASES + file + "." + algorithm),
                        "the repository must receive the " + algorithm + " checksum of " + file);
            }
        }
    }

    @Test
    void snapshotVersionIsUploadedToTheSnapshotsRepository() {
        Outcome outcome = post(releaseJson("1.2.0-SNAPSHOT", revision()));

        assertEquals(201, outcome.status(), "a snapshot release must answer 201");
        assertTrue(repository.keys().stream().anyMatch(key -> key.startsWith(SNAPSHOTS) && SNAPSHOT_JAR.matcher(key).find()),
                "a snapshot version must be uploaded to the snapshots repository, uploaded: " + repository.keys());
    }

    @Test
    void existingVersionAnswers409AndUploadsNothing() {
        repository.store(RELEASES + "eldoria-1.2.0.pom", new byte[]{1});

        Outcome outcome = post(releaseJson("1.2.0", revision()));

        assertEquals(409, outcome.status(), "an existing version must answer 409");
        assertEquals(List.of(RELEASES + "eldoria-1.2.0.pom"), List.copyOf(repository.keys()),
                "nothing may be uploaded for an existing version");
    }

    @Test
    void revisionMismatchAnswers422AndUploadsNothing() {
        Outcome outcome = post(releaseJson("1.2.0", ZERO_REVISION));

        assertEquals(422, outcome.status(), "a revision mismatch must answer 422");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for a revision mismatch");
    }

    @Test
    void revisionOfAnotherSnapshotAnswers422AndUploadsNothing() {
        String staleRevision = digest(snapshot(PROJECT_ID));

        Outcome outcome = post(releaseJson("1.2.0", staleRevision));

        assertEquals(422, outcome.status(), "a revision that is not the digest of the assembled snapshot must answer 422");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for a revision of another snapshot");
    }

    @Test
    void advancementAnswers422NamingItAndUploadsNothing() {
        ProjectFixture fixture = ProjectFixture.project(PROJECT_ID, "eldoria", "Eldoria");
        fixture.advancements().add(obj("id", id(10), "key", "first_page"));
        backend.registerProject(fixture.build());

        Outcome outcome = post(releaseJson("1.2.0", ZERO_REVISION));

        assertEquals(422, outcome.status(), "a project with an advancement must answer 422");
        assertTrue(outcome.body().contains("first_page"), "the body must name the advancement, was: " + outcome.body());
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for unsupported content");
    }

    @Test
    void unknownProjectAnswers422() {
        Outcome outcome = post(releaseJson(UNKNOWN_PROJECT, "1.2.0", revision()));

        assertEquals(422, outcome.status(), "a project unknown to the backend must answer 422");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for an unknown project");
    }

    @Test
    void invalidVersionAnswers400AndUploadsNothing() {
        Outcome outcome = post(releaseJson("1.02.0", revision()));

        assertEquals(400, outcome.status(), "a version with a leading zero must answer 400");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded for an invalid request");
    }

    @Test
    void unavailableBackendAnswers502AndUploadsNothing() {
        backend.setDown(true);

        Outcome outcome = post(releaseJson("1.2.0", ZERO_REVISION));

        assertEquals(502, outcome.status(), "an unavailable backend must answer 502");
        assertTrue(repository.keys().isEmpty(), "nothing may be uploaded when the backend is unavailable");
    }

    @Test
    void rejectionBodyNamesTheReason() {
        Outcome outcome = post(releaseJson("1.2.0", ZERO_REVISION));

        assertTrue(outcome.body().contains("\"reason\":\"REVISION_MISMATCH\""),
                "the error body must name the rejection reason, was: " + outcome.body());
    }

    @Test
    void successfulReleaseNotifiesTheListenerOnce() {
        post(releaseJson("1.2.0", revision()));

        assertEquals(List.of("net.onelitefeather.vulpes.content:eldoria:1.2.0 @ " + revision()), listener.completed(),
                "the listener must be called exactly once with coordinates and revision");
    }

    @Test
    void rejectedReleaseDoesNotNotifyTheListener() {
        post(releaseJson("1.2.0", ZERO_REVISION));

        assertTrue(listener.completed().isEmpty(), "the listener must not be called for a rejected release");
    }

    private String revision() {
        return digest(source.fetch(PROJECT_ID));
    }

    private Outcome post(String json) {
        try {
            HttpResponse<String> response = client.toBlocking()
                    .exchange(HttpRequest.POST("/releases", json)
                            .contentType(MediaType.APPLICATION_JSON_TYPE)
                            .bearerAuth(ReleaseTestProperties.TOKEN), String.class);
            return new Outcome(response.getStatus().getCode(), response.body());
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private static String releaseJson(String version, String revision) {
        return releaseJson(PROJECT_ID, version, revision);
    }

    private static String releaseJson(UUID projectId, String version, String revision) {
        return "{\"projectId\":\"" + projectId + "\",\"project\":\"eldoria\",\"version\":\"" + version
                + "\",\"revision\":\"" + revision + "\",\"createdBy\":\"TheMeinerLP\","
                + "\"createdAt\":\"2026-10-28T12:00:00Z\",\"baseRepository\":\"OneLiteFeatherNET/vulpes-base\","
                + "\"baseBranch\":\"develop\",\"baseCommit\":\"" + ReleaseTestData.BASE_COMMIT + "\"}";
    }

    private record Outcome(int status, String body) {
    }
}
