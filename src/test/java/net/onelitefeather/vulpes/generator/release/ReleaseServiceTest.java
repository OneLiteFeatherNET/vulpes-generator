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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the release flow of {@link ReleaseService}: the existence check, the base verification, the JAR and the
 * publication. The base branch resolver and the base verification are fakes; no repository, backend or clone is used.
 *
 * @author TheMeinerLP
 */
class ReleaseServiceTest {

    private static final ContentCoordinates COORDINATES =
            new ContentCoordinates(ContentCoordinates.GROUP_ID, "eldoria", "1.2.0");
    private static final ContentCoordinates SNAPSHOT_COORDINATES =
            new ContentCoordinates(ContentCoordinates.GROUP_ID, "eldoria", "2.0.0-SNAPSHOT");
    private static final String REPOSITORY_URL = "https://repo.test/onelitefeather-releases";
    private static final BaseConfiguration BASE = new BaseConfiguration(ReleaseTestData.BASE_REPOSITORY, "",
            3, Duration.ofMinutes(2), Duration.ofMinutes(10));

    @TempDir
    Path workDir;

    private final byte[] snapshot = snapshot(PROJECT_ID);
    private final FakeSource source = new FakeSource(snapshot);
    private final FakePublisher publisher = new FakePublisher();
    private final FakeListener listener = new FakeListener();
    private final FakeBaseBranches branches = new FakeBaseBranches();
    private final FakeBaseVerification verification = new FakeBaseVerification();

    @BeforeEach
    void resetFakes() {
        branches.reset();
        verification.reset();
    }

    @Test
    void successfulReleaseReturnsCoordinatesRevisionBaseAndRepositoryUrl() {
        ReleaseRequest request = ReleaseTestData.request();

        ReleaseResult result = service().release(request);

        assertEquals(COORDINATES, result.coordinates(), "result must carry the Maven coordinates");
        assertEquals(request.revision(), result.revision(), "result must carry the requested revision");
        assertEquals(new ReleaseBase(ReleaseTestData.BASE_REPOSITORY, "develop", ReleaseTestData.BASE_COMMIT),
                result.base(), "result must carry the verified base with the full commit");
        assertEquals(REPOSITORY_URL, result.repositoryUrl(), "result must carry the repository of the version");
    }

    @Test
    void successfulReleasePublishesTheJarExactlyOnce() {
        service().release(ReleaseTestData.request());

        assertEquals(1, publisher.publishCount, "the JAR must be published exactly once");
        assertEquals(COORDINATES, publisher.publishedCoordinates, "the JAR must be published under the release coordinates");
    }

    @Test
    void publishedJarContainsTheSnapshotUnchanged() throws IOException {
        service().release(ReleaseTestData.request());

        assertArrayEquals(snapshot, entry(publisher.publishedJar, DataJarBuilder.SNAPSHOT_JSON_NAME),
                "the snapshot entry must be byte-identical to the fetched snapshot");
    }

    @Test
    void publishedJarContainsTheReleaseMetadataWithTheVerifiedBase() throws IOException {
        ReleaseRequest request = ReleaseTestData.request();

        service().release(request);

        assertArrayEquals(new ReleaseJsonWriter().write(request.withBaseBranch("develop"), ReleaseTestData.BASE, "1.0.0"),
                entry(publisher.publishedJar, DataJarBuilder.RELEASE_JSON_NAME),
                "the release entry must be the metadata written for the verified base");
    }

    @Test
    void successfulReleaseNotifiesListenerOnceWithCoordinatesAndRevision() {
        ReleaseRequest request = ReleaseTestData.request();

        service().release(request);

        assertEquals(1, listener.calls.size(), "the listener must be called exactly once");
        assertEquals(COORDINATES, listener.calls.get(0).coordinates(), "listener must get the coordinates");
        assertEquals(request.revision(), listener.calls.get(0).revision(), "listener must get the revision");
    }

    @Test
    void omittedBaseBranchIsResolvedToDevelop() {
        service().release(ReleaseTestData.request().withBaseBranch(null));

        assertEquals("develop", verification.inputs().get(0).branch(), "a release without a branch must use develop");
    }

    @Test
    void unknownBaseBranchFetchesNothingAndPublishesNothing() {
        ReleaseRequest request = ReleaseTestData.request().withBaseBranch("no-such-branch");

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(request));

        assertEquals(ReleaseRejectedException.Reason.UNKNOWN_BASE_BRANCH, e.getReason(), "reason must be UNKNOWN_BASE_BRANCH");
        assertEquals(0, source.fetchCount, "an unknown branch must be refused before the snapshot is fetched");
        assertEquals(0, publisher.publishCount, "an unknown branch must publish nothing");
    }

    @Test
    void compileFailureUploadsNothingAndNotifiesNobody() {
        verification.failWith(new ReleaseRejectedException(ReleaseRejectedException.Reason.COMPILATION_FAILED,
                "does not compile", List.of(new ReleaseDiagnostic("Eldoria.java", 3, "error")), false));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(ReleaseTestData.request()));

        assertEquals(ReleaseRejectedException.Reason.COMPILATION_FAILED, e.getReason(), "reason must be COMPILATION_FAILED");
        assertEquals(0, publisher.publishCount, "a snapshot that does not compile must not be uploaded");
        assertEquals(0, listener.calls.size(), "the listener must not be called for a refused release");
    }

    @Test
    void existingReleaseVersionIsRejectedWithoutFetchingTheSnapshot() {
        publisher.alreadyPublished = true;

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(ReleaseTestData.request()));

        assertEquals(ReleaseRejectedException.Reason.VERSION_EXISTS, e.getReason(), "reason must be VERSION_EXISTS");
        assertEquals(0, source.fetchCount, "the snapshot must not be fetched for an existing version");
        assertEquals(0, branches.callCount(), "the base must not be resolved for an existing version");
    }

    @Test
    void existingReleaseVersionIsRejectedWithoutPublishing() {
        publisher.alreadyPublished = true;

        assertThrows(ReleaseRejectedException.class, () -> service().release(ReleaseTestData.request()));

        assertEquals(0, publisher.publishCount, "nothing must be uploaded for an existing version");
    }

    @Test
    void existingSnapshotVersionIsReplacedNotRefused() {
        publisher.alreadyPublished = true;
        ReleaseRequest request = ReleaseTestData.request("2.0.0-SNAPSHOT", ReleaseTestData.digest(snapshot));

        ReleaseResult result = service().release(request);

        assertEquals(SNAPSHOT_COORDINATES, result.coordinates(), "the snapshot must be published under its coordinates");
        assertEquals(1, publisher.publishCount, "an existing snapshot version must be uploaded again");
        assertEquals(0, publisher.existsCount, "snapshot versions must not be checked for existence");
    }

    @Test
    void invalidRequestCallsNoCollaborator() {
        ReleaseRequest invalid = ReleaseTestData.request("1.02.0", ReleaseTestData.request().revision());

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(invalid));

        assertEquals(ReleaseRejectedException.Reason.INVALID_REQUEST, e.getReason(), "reason must be INVALID_REQUEST");
        assertEquals(0, source.fetchCount, "an invalid request must not reach the snapshot source");
        assertEquals(0, publisher.existsCount, "an invalid request must not reach the repository");
        assertEquals(0, publisher.publishCount, "an invalid request must publish nothing");
    }

    @Test
    void unknownProjectIsRejectedAndNotifiesNobody() {
        source.failWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.UNKNOWN_PROJECT, "unknown");

        assertRejected(ReleaseRejectedException.Reason.UNKNOWN_PROJECT);
    }

    @Test
    void unavailableSourceIsRejectedAndNotifiesNobody() {
        source.failWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE, "down");

        assertRejected(ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE);
    }

    @Test
    void revisionMismatchPublishesNothingAndNotifiesNobody() {
        ReleaseRequest request = ReleaseTestData.request("1.2.0", "sha256:" + "0".repeat(64));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(request));

        assertEquals(ReleaseRejectedException.Reason.REVISION_MISMATCH, e.getReason(), "reason must be REVISION_MISMATCH");
        assertEquals(0, publisher.publishCount, "a rejected snapshot must publish nothing");
        assertEquals(0, listener.calls.size(), "the listener must not be called for a rejected release");
    }

    @Test
    void unsupportedFormatPublishesNothingAndNotifiesNobody() {
        byte[] format2 = ("{\"format\":2,\"projectId\":\"" + PROJECT_ID + "\"}").getBytes(StandardCharsets.UTF_8);
        source.snapshot = format2;
        ReleaseRequest request = ReleaseTestData.request("1.2.0", ReleaseTestData.digest(format2));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(request));

        assertEquals(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, e.getReason(), "reason must be UNSUPPORTED_FORMAT");
        assertEquals(0, publisher.publishCount, "a rejected snapshot must publish nothing");
        assertEquals(0, listener.calls.size(), "the listener must not be called for a rejected release");
    }

    @Test
    void repositoryFailureNotifiesNobody() {
        publisher.failWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, "down");

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(ReleaseTestData.request()));

        assertEquals(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, e.getReason(),
                "a failing upload must be reported as REPOSITORY_UNAVAILABLE");
        assertEquals(1, publisher.publishCount, "the upload must have been attempted once");
        assertEquals(0, listener.calls.size(), "the listener must not be called when the publish fails");
    }

    @Test
    void workspaceIsDeletedAfterSuccessfulRelease() throws IOException {
        service().release(ReleaseTestData.request());

        assertEquals(0, entryCount(workDir), "the temporary build directory must be deleted after success");
    }

    @Test
    void workspaceIsDeletedAfterFailedPublish() throws IOException {
        publisher.failWith = new ReleaseRejectedException(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, "down");

        assertThrows(ReleaseRejectedException.class, () -> service().release(ReleaseTestData.request()));

        assertEquals(0, entryCount(workDir), "the temporary build directory must be deleted after a failed build");
    }

    @Test
    void publishVerifiedPublishesTheGivenBytesWithoutFetchingAgain() {
        service().publishVerified(ReleaseTestData.request().withBaseBranch("develop"), snapshot);

        assertEquals(0, source.fetchCount, "a snapshot that the caller assembled must not be fetched again");
        assertEquals(1, publisher.publishCount, "the assembled snapshot must be published exactly once");
    }

    @Test
    void publishVerifiedRejectsARevisionOfAnotherSnapshot() {
        ReleaseRequest request = ReleaseTestData.request("1.2.0", "sha256:" + "0".repeat(64));

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().publishVerified(request, snapshot));

        assertEquals(ReleaseRejectedException.Reason.REVISION_MISMATCH, e.getReason(), "reason must be REVISION_MISMATCH");
        assertEquals(0, publisher.publishCount, "a snapshot with another revision must publish nothing");
    }

    private void assertRejected(ReleaseRejectedException.Reason expected) {
        ReleaseRequest request = ReleaseTestData.request();

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(request));

        assertEquals(expected, e.getReason(), "unexpected rejection reason");
        assertEquals(0, publisher.publishCount, "a rejected release must publish nothing");
        assertEquals(0, listener.calls.size(), "the listener must not be called for a rejected release");
    }

    private ReleaseService service() {
        return new ReleaseService(source, new SnapshotVerifier(), new ReleaseJsonWriter(), new DataJarBuilder(),
                publisher, listener, branches, verification, BASE, "1.0.0", workDir.toString());
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

    private static long entryCount(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(Objects.requireNonNull(dir))) {
            return entries.count();
        }
    }

    /** Returns a fixed snapshot and records how often it was asked for. */
    private static final class FakeSource implements ReleaseSnapshotSource {
        private byte[] snapshot;
        private RuntimeException failWith;
        private int fetchCount;

        FakeSource(byte[] snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public byte[] fetch(UUID projectId) {
            fetchCount++;
            if (failWith != null) {
                throw failWith;
            }
            return snapshot;
        }
    }

    /** Records the publish calls and keeps a copy of the uploaded JAR. */
    private static final class FakePublisher implements ContentPublisher {
        private boolean alreadyPublished;
        private RuntimeException failWith;
        private int existsCount;
        private int publishCount;
        private ContentCoordinates publishedCoordinates;
        private byte[] publishedJar;

        @Override
        public boolean exists(ContentCoordinates coordinates) {
            existsCount++;
            return alreadyPublished;
        }

        @Override
        public void publish(ContentCoordinates coordinates, Path jarFile) {
            publishCount++;
            publishedCoordinates = coordinates;
            try {
                publishedJar = Files.readAllBytes(jarFile);
            } catch (IOException e) {
                throw new IllegalStateException("fake publisher cannot read " + jarFile, e);
            }
            if (failWith != null) {
                throw failWith;
            }
        }

        @Override
        public String repositoryUrl(ContentCoordinates coordinates) {
            return REPOSITORY_URL;
        }
    }

    private record Call(ContentCoordinates coordinates, String revision) {
    }

    /** Records every completion notification. */
    private static final class FakeListener implements ReleaseCompletionListener {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public void completed(ContentCoordinates coordinates, String revision) {
            calls.add(new Call(coordinates, revision));
        }
    }
}
