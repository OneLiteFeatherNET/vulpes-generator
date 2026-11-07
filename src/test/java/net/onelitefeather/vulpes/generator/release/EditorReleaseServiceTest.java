package net.onelitefeather.vulpes.generator.release;

import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the order of the editor release: the version, the project key, the existence check, the base branch, and only
 * then the snapshot and the base verification. Every collaborator is a fake.
 *
 * @author TheMeinerLP
 */
class EditorReleaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final String REPOSITORY_URL = "https://repo.test/onelitefeather-releases";
    private static final BaseConfiguration BASE = new BaseConfiguration(ReleaseTestData.BASE_REPOSITORY, "",
            3, Duration.ofMinutes(2), Duration.ofMinutes(10));

    @TempDir
    Path workDir;

    private final FakeProjects projects = new FakeProjects();
    private final FakePublisher publisher = new FakePublisher();
    private final FakeBaseBranches branches = new FakeBaseBranches();
    private final FakeBaseVerification verification = new FakeBaseVerification();

    @BeforeEach
    void resetFakes() {
        branches.reset();
        verification.reset();
    }

    @Test
    void existingReleaseVersionIsRefusedBeforeTheSnapshotOrTheBaseIsTouched() {
        publisher.alreadyPublished = true;

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(PROJECT_ID, "2.0.0", null));

        assertEquals(ReleaseRejectedException.Reason.VERSION_EXISTS, e.getReason(), "reason must be VERSION_EXISTS");
        assertEquals(0, projects.assembleCount, "the snapshot must not be assembled for an existing version");
        assertEquals(0, branches.callCount(), "the base branch must not be resolved for an existing version");
        assertTrue(verification.inputs().isEmpty(), "the base must not be checked for an existing version");
        assertEquals(0, publisher.publishCount, "nothing may be uploaded for an existing version");
    }

    @Test
    void invalidVersionIsRefusedBeforeTheBackendIsCalled() {
        assertThrows(ReleaseRejectedException.class, () -> service().release(PROJECT_ID, "1.02.0", null));
        assertThrows(ReleaseRejectedException.class, () -> service().release(PROJECT_ID, "1.2.0-dev.1", null));

        assertEquals(0, projects.keyCount, "an invalid version must not reach the backend");
        assertEquals(0, publisher.existsCount, "an invalid version must not reach the repository");
    }

    @Test
    void unknownProjectIsRefusedBeforeTheSnapshotIsAssembled() {
        projects.failKeyWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, "unknown");

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(PROJECT_ID, "2.0.0", null));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, e.getReason(), "reason must be UNKNOWN_PROJECT");
        assertEquals(0, projects.assembleCount, "an unknown project must not be assembled");
    }

    @Test
    void unknownBaseBranchIsRefusedBeforeTheSnapshotIsAssembled() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(PROJECT_ID, "2.0.0", "no-such-branch"));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_BASE_BRANCH, e.getReason(),
                "reason must be UNKNOWN_BASE_BRANCH");
        assertEquals(0, projects.assembleCount, "an unknown branch must not fetch the snapshot");
        assertEquals(0, publisher.publishCount, "an unknown branch must publish nothing");
    }

    @Test
    void existingSnapshotVersionIsReplacedNotRefused() {
        publisher.alreadyPublished = true;

        ReleaseResult result = service().release(PROJECT_ID, "2.0.0-SNAPSHOT", null);

        assertEquals("2.0.0-SNAPSHOT", result.coordinates().version(), "the snapshot version must be released");
        assertEquals(1, publisher.publishCount, "an existing snapshot version must be uploaded again");
        assertEquals(0, publisher.existsCount, "snapshot versions must not be checked for existence");
    }

    @Test
    void successfulReleaseReturnsTheFullCommitOfTheDefaultBranchAndTheRepository() {
        ReleaseResult result = service().release(PROJECT_ID, "2.0.0", null);

        assertEquals("net.onelitefeather.vulpes.content:eldoria:2.0.0", result.coordinates().toString(),
                "the coordinates must carry the project key");
        assertEquals("develop", result.base().branch(), "an omitted branch must resolve to develop");
        assertEquals(ReleaseTestData.BASE_COMMIT, result.base().commit(), "base.commit must be the full commit");
        assertEquals(REPOSITORY_URL, result.repositoryUrl(), "the repository of the version must be returned");
        assertEquals(1, publisher.publishCount, "the release must be uploaded once");
    }

    @Test
    void successfulReleaseRecordsTheRequestedBranchAndTheCreatorAndClock() throws IOException {
        service().release(PROJECT_ID, "2.0.0", "feature/font");

        String releaseJson = new String(entry(publisher.publishedJar, DataJarBuilder.RELEASE_JSON_NAME),
                StandardCharsets.UTF_8);
        assertTrue(releaseJson.contains("\"branch\":\"feature/font\""), "the release must record the branch: " + releaseJson);
        assertTrue(releaseJson.contains("\"createdBy\":\"stelaris\""), "the creator must be stelaris: " + releaseJson);
        assertTrue(releaseJson.contains("\"createdAt\":\"2026-10-28T12:00:00Z\""),
                "createdAt must come from the clock: " + releaseJson);
    }

    @Test
    void snapshotThatDoesNotCompileUploadsNothing() {
        verification.failWith(new ReleaseRejectedException(ReleaseRejectedException.Reason.COMPILATION_FAILED,
                "does not compile", java.util.List.of(new ReleaseDiagnostic("Eldoria.java", 1, "error")), false));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(PROJECT_ID, "2.0.0", null));

        assertEquals(ReleaseRejectedException.Reason.COMPILATION_FAILED, e.getReason(), "reason must be COMPILATION_FAILED");
        assertEquals(0, publisher.publishCount, "a snapshot that does not compile must not be uploaded");
    }

    @Test
    void snapshotIsVerifiedAgainstTheRequestedBranchAndThePackageOfTheProject() {
        service().release(PROJECT_ID, "2.0.0", "feature/font");

        assertEquals("feature/font", verification.inputs().get(0).branch(),
                "the verification must run against the requested branch");
        assertEquals("net.onelitefeather.vulpes.content.eldoria", verification.inputs().get(0).packageName(),
                "the generated sources must use the default package of the project");
        assertTrue(verification.inputs().get(0).snapshotJson().length > 0, "the verified snapshot must not be empty");
    }

    private EditorReleaseService service() {
        ReleaseService releaseService = new ReleaseService(id -> {
            throw new AssertionError("the editor release must not fetch the snapshot a second time");
        }, new SnapshotVerifier(), new ReleaseJsonWriter(), new DataJarBuilder(), publisher,
                (coordinates, revision) -> { }, branches, verification, BASE, "1.0.0", workDir.toString());
        return new EditorReleaseService(projects, publisher, branches, releaseService,
                Clock.fixed(NOW, ZoneOffset.UTC));
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
        throw new AssertionError("jar has no entry " + name);
    }

    /** Knows one project and records how often the backend was asked. */
    private static final class FakeProjects implements ProjectSource {
        private int keyCount;
        private int assembleCount;
        private RuntimeException failKeyWith;

        @Override
        public String projectKey(UUID projectId) {
            keyCount++;
            if (failKeyWith != null) {
                throw failKeyWith;
            }
            return "eldoria";
        }

        @Override
        public AssembledSnapshot assemble(UUID projectId) {
            assembleCount++;
            return new AssembledSnapshot("eldoria", snapshot(projectId));
        }
    }

    /** Records the publish calls and keeps a copy of the uploaded JAR. */
    private static final class FakePublisher implements ContentPublisher {
        private boolean alreadyPublished;
        private int existsCount;
        private int publishCount;
        private byte[] publishedJar;

        @Override
        public boolean exists(ContentCoordinates coordinates) {
            existsCount++;
            return alreadyPublished;
        }

        @Override
        public void publish(ContentCoordinates coordinates, Path jarFile) {
            publishCount++;
            try {
                publishedJar = Files.readAllBytes(jarFile);
            } catch (IOException e) {
                throw new IllegalStateException("fake publisher cannot read " + jarFile, e);
            }
        }

        @Override
        public String repositoryUrl(ContentCoordinates coordinates) {
            return REPOSITORY_URL;
        }
    }
}
