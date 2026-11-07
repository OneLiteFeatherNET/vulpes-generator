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
 * Tests the validation of a release request and of its version rule. The base branch is not validated here: it is
 * resolved against the repository by the branch resolver.
 *
 * @author TheMeinerLP
 */
class ReleaseRequestTest {

    private static final String VALID_REVISION = "sha256:" + "a".repeat(64);

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
    void retiredDevVersionIsRejectedByTheVersionRuleAlone() {
        assertThrows(ReleaseRejectedException.class, () -> ReleaseRequest.validateVersion("1.2.0-dev.1"),
                "a retired -dev.N version must be refused before any collaborator is called");
    }

    @Test
    void snapshotVersionIsAccepted() {
        assertDoesNotThrow(ReleaseTestData.request("1.2.0-SNAPSHOT", VALID_REVISION)::validate,
                "X.Y.Z-SNAPSHOT must be accepted");
    }

    @Test
    void uppercaseProjectIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "Eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_BRANCH));
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
                ReleaseTestData.BASE_BRANCH));
    }

    @Test
    void missingProjectIdIsRejected() {
        assertInvalid(new ReleaseRequest(null, "eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH,
                ReleaseTestData.BASE_BRANCH));
    }

    @Test
    void missingCreatedAtIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user", null,
                ReleaseTestData.BASE_BRANCH));
    }

    @Test
    void omittedBaseBranchIsAcceptedBecauseTheResolverDefaultsIt() {
        assertDoesNotThrow(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user",
                Instant.EPOCH, null)::validate, "a request without a base branch must pass validation");
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

    @Test
    void withBaseBranchReplacesOnlyTheBranch() {
        ReleaseRequest request = ReleaseTestData.request().withBaseBranch("feature/font");

        assertEquals("feature/font", request.baseBranch(), "the branch must be replaced");
        assertEquals(ReleaseTestData.request().version(), request.version(), "the version must be kept");
    }

    private static void assertInvalid(ReleaseRequest request) {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, request::validate,
                "invalid request must be rejected: " + request);
        assertEquals(ReleaseRejectedException.Reason.INVALID_REQUEST, e.getReason(),
                "an invalid request must be reported as INVALID_REQUEST");
    }
}
