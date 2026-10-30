package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.digest;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.request;
import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.snapshot;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
class SnapshotVerifierTest {

    private final SnapshotVerifier verifier = new SnapshotVerifier();

    @Test
    void matchingDigestFormatAndProjectPass() {
        byte[] snapshot = snapshot(PROJECT_ID);
        assertDoesNotThrow(() -> verifier.verify(request("1.2.0", digest(snapshot)), snapshot),
                "a snapshot with matching digest, format 1 and project must pass");
    }

    @Test
    void digestMismatchIsRejectedAsRevisionMismatch() {
        byte[] snapshot = snapshot(PROJECT_ID);
        byte[] other = snapshot(UUID.randomUUID());
        assertReason(ReleaseRejectedException.Reason.REVISION_MISMATCH, request("1.2.0", digest(other)), snapshot);
    }

    @Test
    void formatTwoIsRejectedAsUnsupportedFormat() {
        byte[] snapshot = json("{\"format\":2,\"projectId\":\"" + PROJECT_ID + "\"}");
        assertReason(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void formatAsStringIsRejectedAsUnsupportedFormat() {
        byte[] snapshot = json("{\"format\":\"1\",\"projectId\":\"" + PROJECT_ID + "\"}");
        assertReason(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void missingFormatIsRejectedAsUnsupportedFormat() {
        byte[] snapshot = json("{\"projectId\":\"" + PROJECT_ID + "\"}");
        assertReason(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void formatInsideNestedObjectDoesNotCount() {
        byte[] snapshot = json("{\"meta\":{\"format\":1},\"projectId\":\"" + PROJECT_ID + "\"}");
        assertReason(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void unparsableSnapshotIsRejectedAsUnsupportedFormat() {
        byte[] snapshot = json("{\"format\":1,");
        assertReason(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void foreignProjectIdIsRejectedAsProjectMismatch() {
        byte[] snapshot = snapshot(UUID.randomUUID());
        assertReason(ReleaseRejectedException.Reason.PROJECT_MISMATCH, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void missingProjectIdIsRejectedAsProjectMismatch() {
        byte[] snapshot = json("{\"format\":1,\"items\":[]}");
        assertReason(ReleaseRejectedException.Reason.PROJECT_MISMATCH, request("1.2.0", digest(snapshot)), snapshot);
    }

    @Test
    void digestIsCheckedBeforeFormat() {
        byte[] snapshot = json("{\"format\":2,\"projectId\":\"" + PROJECT_ID + "\"}");
        assertReason(ReleaseRejectedException.Reason.REVISION_MISMATCH,
                request("1.2.0", "sha256:" + "0".repeat(64)), snapshot);
    }

    private void assertReason(ReleaseRejectedException.Reason expected, ReleaseRequest request, byte[] snapshot) {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> verifier.verify(request, snapshot), "snapshot must be rejected");
        assertEquals(expected, e.getReason(), "unexpected rejection reason");
    }

    private static byte[] json(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
