package net.onelitefeather.vulpes.generator.release;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Publishes a content JAR to the in-process fake Maven repository.
 *
 * @author TheMeinerLP
 */
@MicronautTest(environments = "release-test")
class MavenContentPublisherIntegrationTest {

    private static final String RELEASES_PREFIX = "onelitefeather-releases/";
    private static final String SNAPSHOTS_PREFIX = "onelitefeather-snapshots/";
    private static final String PATH = "net/onelitefeather/vulpes/content/eldoria/1.2.0/eldoria-1.2.0";
    private static final String[] CHECKSUMS = {"md5", "sha1", "sha256", "sha512"};

    @Inject
    ContentPublisher publisher;

    @Inject
    FakeMavenRepository repository;

    @TempDir
    Path workDir;

    @BeforeEach
    void resetRepository() {
        repository.reset();
    }

    @Test
    void publishUploadsJarPomModuleAndTheirChecksums() throws IOException {
        publisher.publish(ContentCoordinates.forRelease("eldoria", "1.2.0"), buildJar("1.2.0"));

        for (String file : new String[]{".jar", ".pom", ".module"}) {
            assertTrue(repository.contains(RELEASES_PREFIX + PATH + file), "repository must receive " + file);
            for (String algorithm : CHECKSUMS) {
                assertTrue(repository.contains(RELEASES_PREFIX + PATH + file + "." + algorithm),
                        "repository must receive the " + algorithm + " checksum of " + file);
            }
        }
    }

    @Test
    void uploadedJarIsTheGivenJarBytes() throws IOException {
        Path jar = buildJar("1.2.0");

        publisher.publish(ContentCoordinates.forRelease("eldoria", "1.2.0"), jar);

        assertArrayEquals(Files.readAllBytes(jar), repository.get(RELEASES_PREFIX + PATH + ".jar"),
                "the uploaded JAR must be the JAR that was built");
    }

    @Test
    void uploadedPomIsTheWrittenPom() throws IOException {
        publisher.publish(ContentCoordinates.forRelease("eldoria", "1.2.0"), buildJar("1.2.0"));

        assertArrayEquals(ContentPomWriter.write(ContentCoordinates.forRelease("eldoria", "1.2.0"))
                        .getBytes(StandardCharsets.UTF_8),
                repository.get(RELEASES_PREFIX + PATH + ".pom"), "the uploaded POM must match the POM writer");
    }

    @Test
    void devVersionIsUploadedToTheSnapshotsRepository() throws IOException {
        publisher.publish(ContentCoordinates.forRelease("eldoria", "1.2.0-dev.3"), buildJar("1.2.0-dev.3"));

        assertTrue(repository.contains(SNAPSHOTS_PREFIX
                        + "net/onelitefeather/vulpes/content/eldoria/1.2.0-dev.3/eldoria-1.2.0-dev.3.jar"),
                "a dev version must be uploaded to the snapshots repository");
        assertFalse(repository.keys().stream().anyMatch(key -> key.startsWith(RELEASES_PREFIX)),
                "a dev version must not touch the releases repository");
    }

    @Test
    void uploadsCarryTheConfiguredBasicAuthentication() throws IOException {
        publisher.publish(ContentCoordinates.forRelease("eldoria", "1.2.0"), buildJar("1.2.0"));

        String expected = "Basic " + Base64.getEncoder().encodeToString("test-user:test-secret".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, repository.lastAuthorization(), "uploads must use the configured credentials");
    }

    @Test
    void existsIsFalseBeforePublishAndTrueAfterwards() throws IOException {
        ContentCoordinates coordinates = ContentCoordinates.forRelease("eldoria", "1.2.0");
        assertFalse(publisher.exists(coordinates), "a version that was never published must not exist");

        publisher.publish(coordinates, buildJar("1.2.0"));

        assertTrue(publisher.exists(coordinates), "a published version must exist");
    }

    private Path buildJar(String version) throws IOException {
        Path jar = Files.createTempDirectory(workDir, "jar-").resolve("eldoria-" + version + ".jar");
        new DataJarBuilder().build(jar, new ReleaseJsonWriter()
                .write(ReleaseTestData.request(version, ReleaseTestData.digest(snapshot(PROJECT_ID))), "1.0.0"),
                snapshot(PROJECT_ID));
        return jar;
    }
}
