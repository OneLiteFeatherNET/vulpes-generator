package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
class ReleaseServiceTest {

    private static final ContentCoordinates COORDINATES =
            new ContentCoordinates(ContentCoordinates.GROUP_ID, "eldoria", "1.2.0");

    @TempDir
    Path workDir;

    private final byte[] snapshot = snapshot(PROJECT_ID);
    private final FakeSource source = new FakeSource(snapshot);
    private final FakePublisher publisher = new FakePublisher();
    private final FakeListener listener = new FakeListener();

    @Test
    void successfulReleaseReturnsCoordinatesAndRevision() {
        ReleaseRequest request = ReleaseTestData.request();

        ReleaseResult result = service().release(request);

        assertEquals(COORDINATES, result.coordinates(), "result must carry the Maven coordinates");
        assertEquals(request.revision(), result.revision(), "result must carry the requested revision");
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
    void publishedJarContainsTheReleaseMetadataOfTheRequest() throws IOException {
        ReleaseRequest request = ReleaseTestData.request();

        service().release(request);

        assertArrayEquals(new ReleaseJsonWriter().write(request, "1.0.0"),
                entry(publisher.publishedJar, DataJarBuilder.RELEASE_JSON_NAME),
                "the release entry must be the metadata written for the request");
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
    void existingVersionIsRejectedWithoutFetchingTheSnapshot() {
        publisher.alreadyPublished = true;

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> service().release(ReleaseTestData.request()));

        assertEquals(ReleaseRejectedException.Reason.VERSION_EXISTS, e.getReason(), "reason must be VERSION_EXISTS");
        assertEquals(0, source.fetchCount, "the snapshot must not be fetched for an existing version");
    }

    @Test
    void existingVersionIsRejectedWithoutPublishing() {
        publisher.alreadyPublished = true;

        assertThrows(ReleaseRejectedException.class, () -> service().release(ReleaseTestData.request()));

        assertEquals(0, publisher.publishCount, "nothing must be uploaded for an existing version");
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

    private void assertRejected(ReleaseRejectedException.Reason expected) {
        ReleaseRequest request = ReleaseTestData.request();

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, () -> service().release(request));

        assertEquals(expected, e.getReason(), "unexpected rejection reason");
        assertEquals(0, publisher.publishCount, "a rejected release must publish nothing");
        assertEquals(0, listener.calls.size(), "the listener must not be called for a rejected release");
    }

    private ReleaseService service() {
        return new ReleaseService(source, new SnapshotVerifier(), new ReleaseJsonWriter(), new DataJarBuilder(),
                publisher, listener, "1.0.0", workDir.toString());
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
