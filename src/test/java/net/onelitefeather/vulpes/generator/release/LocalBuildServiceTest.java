package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the in-memory local build: the version, the provenance, the revision and the layout of the zip. The base and
 * the backend are fakes.
 *
 * @author TheMeinerLP
 */
class LocalBuildServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private static final BaseConfiguration BASE = new BaseConfiguration(ReleaseTestData.BASE_REPOSITORY, "",
            3, Duration.ofMinutes(2), Duration.ofMinutes(10));

    private final FakeProjects projects = new FakeProjects();
    private final FakeBaseBranches branches = new FakeBaseBranches();
    private final FakeBaseVerification verification = new FakeBaseVerification();

    @BeforeEach
    void resetFakes() {
        branches.reset();
        verification.reset();
    }

    @Test
    void localBuildHoldsTheReleaseAndTheSnapshotOfTheProject() throws IOException {
        LocalBuild build = service().build(PROJECT_ID, null);

        assertEquals("eldoria", build.projectKey(), "the file name must carry the project key");
        assertEquals(List.of("vulpes/eldoria/release.json", "vulpes/eldoria/snapshot.json"), entryNames(build.zip()),
                "the zip must hold exactly the two metadata entries");
    }

    @Test
    void releaseJsonIsTheLocalVersionAgainstTheCompiledCommit() throws IOException {
        JsonNode release = release(service().build(PROJECT_ID, null));

        assertEquals("0.0.0-SNAPSHOT", release.get("version").asText(), "a local build has the version 0.0.0-SNAPSHOT");
        assertEquals(ReleaseTestData.BASE_COMMIT, release.get("base").get("commit").asText(),
                "base.commit must be the commit that was compiled");
        assertEquals("develop", release.get("base").get("branch").asText(), "an omitted branch must be develop");
        assertEquals("local", release.get("provenance").get("createdBy").asText(), "createdBy must be local");
        assertEquals("2026-11-07T10:00:00Z", release.get("provenance").get("createdAt").asText(),
                "createdAt must come from the clock");
    }

    @Test
    void revisionIsTheSha256OfTheSnapshotJson() throws IOException {
        LocalBuild build = service().build(PROJECT_ID, "feature/font");

        assertEquals(ReleaseTestData.digest(projects.snapshot), release(build).get("revision").asText(),
                "the revision must be the SHA-256 of snapshot.json");
        assertEquals("feature/font", release(build).get("base").get("branch").asText(),
                "the requested branch must be recorded");
    }

    @Test
    void snapshotEntryIsUnchanged() throws IOException {
        LocalBuild build = service().build(PROJECT_ID, null);

        assertArrayEquals(projects.snapshot, entry(build.zip(), "vulpes/eldoria/snapshot.json"),
                "snapshot.json must be the snapshot bytes of the backend");
    }

    @Test
    void unknownProjectIsRefusedBeforeTheBaseIsChecked() {
        projects.failWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, "unknown");

        assertThrows(ReleaseRejectedException.class, () -> service().build(PROJECT_ID, null));

        assertTrue(verification.inputs().isEmpty(), "the base must not be checked for an unknown project");
    }

    @Test
    void unknownBaseBranchIsRefusedBeforeTheSnapshotIsAssembled() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().build(PROJECT_ID, "no-such-branch"));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_BASE_BRANCH, e.getReason(), "reason must be UNKNOWN_BASE_BRANCH");
        assertEquals(0, projects.assembleCount, "an unknown branch must not fetch the snapshot");
    }

    @Test
    void compilationFailureGivesNoZip() {
        verification.failWith(new ReleaseRejectedException(ReleaseRejectedException.Reason.COMPILATION_FAILED,
                "does not compile", List.of(new ReleaseDiagnostic("Eldoria.java", 7, "cannot find symbol")), false));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().build(PROJECT_ID, null));

        assertEquals(ReleaseRejectedException.Reason.COMPILATION_FAILED, e.getReason(), "reason must be COMPILATION_FAILED");
        assertEquals(1, e.getDiagnostics().size(), "the diagnostics must be carried by the refusal");
    }

    private LocalBuildService service() {
        return new LocalBuildService(projects, branches, verification, new ReleaseJsonWriter(), BASE,
                Clock.fixed(NOW, ZoneOffset.UTC), "1.0.0");
    }

    private static JsonNode release(LocalBuild build) throws IOException {
        return ProjectFixture.JSON.readTree(entry(build.zip(), "vulpes/eldoria/release.json"));
    }

    private static List<String> entryNames(byte[] zip) throws IOException {
        List<String> names = new java.util.ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private static byte[] entry(byte[] zip, String name) throws IOException {
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return in.readAllBytes();
                }
            }
        }
        throw new AssertionError("the zip has no entry " + name);
    }

    /** Knows one project and records how often the snapshot was assembled. */
    private static final class FakeProjects implements ProjectSource {
        private final byte[] snapshot = snapshot(PROJECT_ID);
        private int assembleCount;
        private RuntimeException failWith;

        @Override
        public String projectKey(UUID projectId) {
            if (failWith != null) {
                throw failWith;
            }
            return "eldoria";
        }

        @Override
        public AssembledSnapshot assemble(UUID projectId) {
            assembleCount++;
            return new AssembledSnapshot("eldoria", snapshot);
        }
    }
}
