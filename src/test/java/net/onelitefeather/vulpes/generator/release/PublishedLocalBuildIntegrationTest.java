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
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code GET /projects/{projectId}/releases/{version}/local-build}: the published data JAR, read from the fake
 * repositories, becomes a local build zip whose JSON entries are copied unchanged.
 *
 * @author TheMeinerLP
 */
@MicronautTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublishedLocalBuildIntegrationTest implements TestPropertyProvider {

    private static final UUID UNKNOWN_PROJECT = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    private static final String RELEASES = "onelitefeather-releases/net/onelitefeather/vulpes/content/eldoria/";
    private static final String SNAPSHOTS = "onelitefeather-snapshots/net/onelitefeather/vulpes/content/eldoria/";

    @TempDir
    Path workDir;

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
    void publishedVersionAnswers200WithTheLayoutOfALocalBuild() throws IOException {
        byte[] jar = publishedJar("1.1.0");
        repository.store(RELEASES + "1.1.0/eldoria-1.1.0.jar", jar);

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases/1.1.0/local-build");

        assertEquals(200, outcome.status(), "a published version must answer 200");
        assertEquals("application/zip", outcome.contentType(), "the media type must be application/zip");
        assertEquals("attachment; filename=\"eldoria-1.1.0.zip\"", outcome.contentDisposition(),
                "the download must be named after the project and the version");
        assertEquals(List.of("vulpes/eldoria/release.json", "vulpes/eldoria/snapshot.json"),
                List.copyOf(entries(outcome.body()).keySet()), "the zip must hold exactly the two metadata entries");
    }

    @Test
    void jsonEntriesAreByteIdenticalToThePublishedJar() throws IOException {
        byte[] jar = publishedJar("1.1.0");
        repository.store(RELEASES + "1.1.0/eldoria-1.1.0.jar", jar);

        Map<String, byte[]> zip = entries(get("/projects/" + PROJECT_ID + "/releases/1.1.0/local-build").body());

        assertArrayEquals(entry(jar, DataJarBuilder.RELEASE_JSON_NAME), zip.get("vulpes/eldoria/release.json"),
                "release.json must be copied byte for byte from the published JAR");
        assertArrayEquals(entry(jar, DataJarBuilder.SNAPSHOT_JSON_NAME), zip.get("vulpes/eldoria/snapshot.json"),
                "snapshot.json must be copied byte for byte from the published JAR");
    }

    @Test
    void unpublishedVersionAnswers404() {
        assertEquals(404, get("/projects/" + PROJECT_ID + "/releases/9.9.9/local-build").status(),
                "a version that is not published must answer 404");
    }

    @Test
    void snapshotVersionIsDownloadedFromItsTimestampedFile() throws IOException {
        repository.store(SNAPSHOTS + "1.1.0-SNAPSHOT/maven-metadata.xml", ("<metadata><versioning><snapshotVersions>"
                + "<snapshotVersion><extension>jar</extension><value>1.1.0-20261107.221202-3</value></snapshotVersion>"
                + "</snapshotVersions></versioning></metadata>").getBytes(StandardCharsets.UTF_8));
        repository.store(SNAPSHOTS + "1.1.0-SNAPSHOT/eldoria-1.1.0-20261107.221202-3.jar", publishedJar("1.1.0-SNAPSHOT"));

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases/1.1.0-SNAPSHOT/local-build");

        assertEquals(200, outcome.status(), "a published snapshot must answer 200");
        assertEquals("attachment; filename=\"eldoria-1.1.0-SNAPSHOT.zip\"", outcome.contentDisposition(),
                "the download of a snapshot is named after its version");
    }

    @Test
    void jarWhoseRevisionDoesNotMatchAnswers502WithNoZip() throws IOException {
        repository.store(RELEASES + "1.1.0/eldoria-1.1.0.jar", jarWithRevision("1.1.0", "sha256:" + "0".repeat(64)));

        Outcome outcome = get("/projects/" + PROJECT_ID + "/releases/1.1.0/local-build");

        assertEquals(502, outcome.status(), "a corrupt published JAR must answer 502");
        assertTrue(outcome.text().contains("PUBLISHED_JAR_CORRUPT"), "the body must name the reason: " + outcome.text());
    }

    @Test
    void versionPathThatIsNotAVersionAnswers400WithoutContactingAnything() {
        assertEquals(400, get("/projects/" + PROJECT_ID + "/releases/1.0/local-build").status(),
                "a version path with two parts must answer 400");
        assertEquals(0, repository.requestCount(), "no repository may be contacted for an invalid version");
        assertEquals(0, backend.requestCount(), "the backend must not be contacted for an invalid version");
    }

    @Test
    void retiredDevVersionAnswers400() {
        assertEquals(400, get("/projects/" + PROJECT_ID + "/releases/1.2.0-dev.1/local-build").status(),
                "a -dev.N version is retired and must answer 400");
    }

    @Test
    void unknownProjectAnswers422WithoutContactingTheRepositories() {
        assertEquals(422, get("/projects/" + UNKNOWN_PROJECT + "/releases/1.1.0/local-build").status(),
                "a project unknown to the backend must answer 422");
        assertEquals(0, repository.requestCount(), "no repository may be contacted for an unknown project");
    }

    /** Builds a data JAR of the sample project whose release.json names the snapshot's own revision. */
    private byte[] publishedJar(String version) throws IOException {
        return jarWithRevision(version, ReleaseTestData.digest(sampleSnapshot()));
    }

    private byte[] jarWithRevision(String version, String revision) throws IOException {
        byte[] snapshot = sampleSnapshot();
        ReleaseRequest request = new ReleaseRequest(PROJECT_ID, "eldoria", version, revision, "TheMeinerLP",
                Instant.parse("2026-10-28T12:00:00Z"), ReleaseTestData.BASE_BRANCH);
        Path jar = workDir.resolve("data-" + version + "-" + revision.hashCode() + ".jar");
        new DataJarBuilder().build(jar, new ReleaseJsonWriter().write(request, ReleaseTestData.BASE, "1.0.0"), snapshot);
        return Files.readAllBytes(jar);
    }

    private static byte[] sampleSnapshot() {
        return new ProjectSnapshotMapper(List.of("minecraft", "olf")).map(SampleProject.load().build()).bytes();
    }

    private static byte[] entry(byte[] jar, String name) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new AssertionError("the JAR has no entry " + name);
    }

    private static Map<String, byte[]> entries(byte[] zipBytes) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                entries.put(entry.getName(), zip.readAllBytes());
            }
        }
        return entries;
    }

    private Outcome get(String uri) {
        try {
            HttpResponse<byte[]> response = client.toBlocking().exchange(HttpRequest.GET(uri), byte[].class);
            return new Outcome(response.getStatus().getCode(), response.getContentType().map(MediaType::toString).orElse(""),
                    response.header("Content-Disposition"), response.body(), "");
        } catch (HttpClientResponseException e) {
            return new Outcome(e.getStatus().getCode(), "", null, null,
                    e.getResponse().getBody(String.class).orElse(""));
        }
    }

    private record Outcome(int status, String contentType, String contentDisposition, byte[] body, String text) {
    }
}
