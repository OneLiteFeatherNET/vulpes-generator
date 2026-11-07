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

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GET /projects/{projectId}/releases} against the fake backend and the two fake repositories.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReleaseListingControllerIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String RELEASES = "onelitefeather-releases/net/onelitefeather/vulpes/content/example-pack/";
    private static final String SNAPSHOTS = "onelitefeather-snapshots/net/onelitefeather/vulpes/content/example-pack/";
    private static final String GROUP_PATH = "net/onelitefeather/vulpes/content/example-pack/";

    private String releasesUrl;
    private String snapshotsUrl;

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = ReleaseTestProperties.onFreePort();
        releasesUrl = properties.get("vulpes.content.maven.releases-url");
        snapshotsUrl = properties.get("vulpes.content.maven.snapshots-url");
        return properties;
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
        backend.putObject("project/" + PROJECT_ID, FakeBackend.project(PROJECT_ID, "example-pack", "Example Pack"));
    }

    @Test
    void bothChannelsAreListedWithCoordinatesAndRepositories() throws Exception {
        publishReleases("1.0.0", "1.1.0");
        publishSnapshot("1.1.0-SNAPSHOT", "1.1.0-20261107.221202-3");

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases");

        assertEquals(200, outcome.status(), "a published project must answer 200");
        JsonNode versions = json(outcome.body()).get("versions");
        assertEquals(3, versions.size(), "the list must hold both releases and the snapshot: " + outcome.body());
        JsonNode release = find(versions, "1.0.0");
        assertEquals("release", release.get("channel").asText(), "1.0.0 is in the release channel");
        assertEquals("net.onelitefeather.vulpes.content:example-pack:1.0.0", release.get("coordinates").asText(),
                "the coordinates must carry the project key");
        assertEquals(releasesUrl, release.get("repositoryUrl").asText(), "1.0.0 lives in the releases repository");
        assertEquals(releasesUrl + "/" + GROUP_PATH + "1.0.0/example-pack-1.0.0.jar", release.get("jarUrl").asText(),
                "a release jar has the plain file name");
        JsonNode snapshot = find(versions, "1.1.0-SNAPSHOT");
        assertEquals("snapshot", snapshot.get("channel").asText(), "1.1.0-SNAPSHOT is in the snapshot channel");
        assertEquals(snapshotsUrl, snapshot.get("repositoryUrl").asText(), "the snapshot lives in the snapshots repository");
    }

    @Test
    void snapshotJarUrlPointsAtTheTimestampedFile() throws Exception {
        publishSnapshot("1.1.0-SNAPSHOT", "1.1.0-20261107.221202-3");

        JsonNode snapshot = find(json(get("/projects/" + PROJECT_ID + "/releases").body()).get("versions"), "1.1.0-SNAPSHOT");

        assertEquals(snapshotsUrl + "/" + GROUP_PATH + "1.1.0-SNAPSHOT/example-pack-1.1.0-20261107.221202-3.jar",
                snapshot.get("jarUrl").asText(), "Maven Resolver deploys the snapshot under its timestamped file name");
    }

    @Test
    void snapshotVersionIsListedOnceEvenIfTheMetadataRepeatsIt() throws Exception {
        repository.store(SNAPSHOTS + "maven-metadata.xml", metadata("1.1.0-SNAPSHOT", "1.1.0-SNAPSHOT"));

        JsonNode versions = json(get("/projects/" + PROJECT_ID + "/releases").body()).get("versions");

        assertEquals(1, versions.size(), "a republished snapshot must appear once: " + versions);
    }

    @Test
    void latestIsTheNewestVersionByItsNumbers() throws Exception {
        publishReleases("1.9.0", "1.10.0");

        JsonNode latest = json(get("/projects/" + PROJECT_ID + "/releases").body()).get("latest");

        assertEquals("1.10.0", latest.get("release").asText(), "1.10.0 is newer than 1.9.0");
        assertTrue(latest.get("snapshot").isNull(), "a channel without versions is null");
    }

    @Test
    void latestHoldsTheNewestOfEachChannel() throws Exception {
        publishReleases("1.1.0");
        publishSnapshot("1.1.0-SNAPSHOT", "1.1.0-20261107.221202-3");

        JsonNode latest = json(get("/projects/" + PROJECT_ID + "/releases").body()).get("latest");

        assertEquals("1.1.0", latest.get("release").asText(), "the release channel has its own latest");
        assertEquals("1.1.0-SNAPSHOT", latest.get("snapshot").asText(), "the snapshot channel has its own latest");
    }

    @Test
    void projectNeverPublishedAnswersAnEmptyList() {
        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases");

        assertEquals(200, outcome.status(), "both repositories answering 404 must answer 200");
        assertEquals(0, json(outcome.body()).get("versions").size(), "the list must be empty");
    }

    @Test
    void releasesRepositoryFailureAnswers502NamingTheRepository() {
        repository.failUnder("onelitefeather-releases/", 500);

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases");

        assertEquals(502, outcome.status(), "a failing repository must answer 502");
        assertTrue(outcome.body().contains("releases repository"), "the message must name the repository: " + outcome.body());
        assertFalse(outcome.body().contains("\"versions\""), "no partial list may be returned");
    }

    @Test
    void snapshotsRepositoryFailureAnswers502NamingTheRepository() {
        publishReleases("1.0.0");
        repository.failUnder("onelitefeather-snapshots/", 503);

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases");

        assertEquals(502, outcome.status(), "a failing snapshots repository must answer 502");
        assertTrue(outcome.body().contains("snapshots repository"), "the message must name the snapshots repository: " + outcome.body());
    }

    @Test
    void unreadableMetadataAnswers502() {
        repository.store(RELEASES + "maven-metadata.xml", "<metadata><versioning>".getBytes(StandardCharsets.UTF_8));

        assertEquals(502, get("/projects/" + PROJECT_ID + "/releases").status(), "unreadable metadata must answer 502");
    }

    @Test
    void responseHoldsNoCredential() {
        publishReleases("1.0.0");
        publishSnapshot("1.1.0-SNAPSHOT", "1.1.0-20261107.221202-3");

        String body = get("/projects/" + PROJECT_ID + "/releases").body();

        assertFalse(body.contains("test-secret") || body.contains("test-user") || body.contains("Authorization"),
                "no credential may appear in a response: " + body);
    }

    @Test
    void malformedProjectIdAnswers400WithoutContactingAnything() {
        assertEquals(400, get("/projects/Eldoria/releases").status(), "a project id that is not a UUID must answer 400");
        assertEquals(0, repository.requestCount(), "the repositories must not be contacted");
        assertEquals(0, backend.requestCount(), "the backend must not be contacted");
    }

    @Test
    void unknownProjectAnswers422WithoutContactingTheRepositories() {
        Outcome outcome = get("/projects/" + UNKNOWN_PROJECT + "/releases");

        assertEquals(422, outcome.status(), "a project unknown to the backend must answer 422");
        assertEquals(0, repository.requestCount(), "no repository may be contacted for an unknown project");
    }

    @Test
    void invalidKeyFromTheBackendAnswers422WithoutContactingTheRepositories() {
        backend.putObject("project/" + PROJECT_ID, FakeBackend.project(PROJECT_ID, "Eldoria", "Eldoria"));

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases");

        assertEquals(422, outcome.status(), "a key that is not a valid artifact id must answer 422");
        assertEquals(0, repository.requestCount(), "no repository may be contacted for an invalid key");
    }

    private void publishReleases(String... versions) {
        for (String version : versions) {
            repository.store(RELEASES + version + "/example-pack-" + version + ".jar", new byte[]{1});
        }
        repository.store(RELEASES + "maven-metadata.xml", metadata(versions));
    }

    private void publishSnapshot(String version, String timestamped) {
        repository.store(SNAPSHOTS + version + "/maven-metadata.xml", snapshotMetadata(version, timestamped));
        repository.store(SNAPSHOTS + "maven-metadata.xml", metadata(version));
    }

    private static byte[] metadata(String... versions) {
        StringBuilder xml = new StringBuilder("<metadata><groupId>net.onelitefeather.vulpes.content</groupId>"
                + "<artifactId>example-pack</artifactId><versioning><versions>");
        for (String version : versions) {
            xml.append("<version>").append(version).append("</version>");
        }
        return xml.append("</versions></versioning></metadata>").toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] snapshotMetadata(String version, String timestamped) {
        return ("<metadata><version>" + version + "</version><versioning><snapshotVersions>"
                + "<snapshotVersion><extension>jar</extension><value>" + timestamped + "</value></snapshotVersion>"
                + "</snapshotVersions></versioning></metadata>").getBytes(StandardCharsets.UTF_8);
    }

    private static JsonNode find(JsonNode versions, String version) {
        for (JsonNode node : versions) {
            if (node.get("version").asText().equals(version)) {
                return node;
            }
        }
        throw new AssertionError("version " + version + " is not listed in " + versions);
    }

    private static JsonNode json(String body) {
        try {
            return ProjectFixture.JSON.readTree(body);
        } catch (Exception e) {
            throw new AssertionError("the body is not JSON: " + body, e);
        }
    }

    private Outcome get(String uri) {
        try {
            HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String.class);
            return new Outcome(response.getStatus().getCode(), response.body());
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private record Outcome(int status, String body) {
    }
}
