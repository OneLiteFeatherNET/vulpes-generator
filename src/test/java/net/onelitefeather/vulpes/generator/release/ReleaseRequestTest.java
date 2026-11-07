package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the validation of a release request, including its base and its version rule.
 *
 * @author TheMeinerLP
 */
class ReleaseRequestTest {

    private static final String VALID_REVISION = "sha256:" + "a".repeat(64);
    private static final String VALID_COMMIT = "0123456789abcdef0123456789abcdef01234567";

    @Test
    void validRequestIsAccepted() {
        assertDoesNotThrow(ReleaseTestData.request()::validate, "a well-formed request must pass validation");
    }

    @Test
    void versionWithLeadingZeroIsRejected() {
        assertInvalid(ReleaseTestData.request("1.02.0", VALID_REVISION));
    }

    @Test
    void snapshotVersionWithLeadingZeroIsRejected() {
        assertInvalid(ReleaseTestData.request("1.0.01-SNAPSHOT", VALID_REVISION));
    }

    @Test
    void versionWithoutPatchNumberIsRejected() {
        assertInvalid(ReleaseTestData.request("1.2", VALID_REVISION));
    }

    @Test
    void retiredDevVersionIsRejected() {
        assertInvalid(ReleaseTestData.request("1.2.0-dev.3", VALID_REVISION));
    }

    @Test
    void snapshotVersionIsAccepted() {
        assertDoesNotThrow(ReleaseTestData.request("1.2.0-SNAPSHOT", VALID_REVISION)::validate,
                "X.Y.Z-SNAPSHOT must be a valid version");
    }

    @Test
    void uppercaseProjectIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "Eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_REPOSITORY, ReleaseTestData.BASE_BRANCH, ReleaseTestData.BASE_COMMIT));
    }

    @Test
    void revisionWithoutSha256PrefixIsRejected() {
        assertInvalid(ReleaseTestData.request("1.2.0", "a".repeat(64)));
    }

    @Test
    void revisionWithUppercaseHexIsRejected() {
        assertInvalid(ReleaseTestData.request("1.2.0", "sha256:" + "A".repeat(64)));
    }

    @Test
    void blankCreatedByIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "  ", Instant.EPOCH,
                ReleaseTestData.BASE_REPOSITORY, ReleaseTestData.BASE_BRANCH, VALID_COMMIT));
    }

    @Test
    void missingProjectIdIsRejected() {
        assertInvalid(new ReleaseRequest(null, "eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_REPOSITORY, ReleaseTestData.BASE_BRANCH, VALID_COMMIT));
    }

    @Test
    void missingCreatedAtIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user", null,
                ReleaseTestData.BASE_REPOSITORY, ReleaseTestData.BASE_BRANCH, VALID_COMMIT));
    }

    @Test
    void abbreviatedBaseCommitIsRejected() {
        assertInvalid(withBase(VALID_COMMIT.substring(0, 12)));
    }

    @Test
    void uppercaseBaseCommitIsRejected() {
        assertInvalid(withBase(VALID_COMMIT.toUpperCase(java.util.Locale.ROOT)));
    }

    @Test
    void missingBaseCommitIsRejected() {
        assertInvalid(withBase(null));
    }

    @Test
    void blankBaseBranchIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_REPOSITORY, " ", VALID_COMMIT));
    }

    @Test
    void snapshotVersionIsRecognised() {
        assertTrue(ReleaseTestData.request("1.2.0-SNAPSHOT", VALID_REVISION).isSnapshotVersion(),
                "1.2.0-SNAPSHOT must be a snapshot version");
    }

    @Test
    void releaseVersionIsNotSnapshotVersion() {
        assertFalse(ReleaseTestData.request("1.2.0", VALID_REVISION).isSnapshotVersion(),
                "1.2.0 must not be a snapshot version");
    }

    private static ReleaseRequest withBase(String commit) {
        return new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_REPOSITORY, ReleaseTestData.BASE_BRANCH, commit);
    }

    private static void assertInvalid(ReleaseRequest request) {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, request::validate,
                "invalid request must be rejected: " + request);
        assertEquals(ReleaseRejectedException.Reason.INVALID_REQUEST, e.getReason(),
                "invalid request must be reported as INVALID_REQUEST");
    }
}
