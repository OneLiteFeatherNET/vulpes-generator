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
 * Tests for the release package.
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
    void devVersionWithLeadingZeroIsRejected() {
        assertInvalid(ReleaseTestData.request("1.0.0-dev.01", VALID_REVISION));
    }

    @Test
    void versionWithoutPatchNumberIsRejected() {
        assertInvalid(ReleaseTestData.request("1.2", VALID_REVISION));
    }

    @Test
    void uppercaseProjectIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "Eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH));
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
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "  ", Instant.EPOCH));
    }

    @Test
    void missingProjectIdIsRejected() {
        assertInvalid(new ReleaseRequest(null, "eldoria", "1.2.0", VALID_REVISION, "user", Instant.EPOCH));
    }

    @Test
    void missingCreatedAtIsRejected() {
        assertInvalid(new ReleaseRequest(UUID.randomUUID(), "eldoria", "1.2.0", VALID_REVISION, "user", null));
    }

    @Test
    void devVersionIsRecognised() {
        assertTrue(ReleaseTestData.request("1.2.0-dev.3", VALID_REVISION).isDevVersion(),
                "1.2.0-dev.3 must be a dev version");
    }

    @Test
    void releaseVersionIsNotDevVersion() {
        assertFalse(ReleaseTestData.request("1.2.0", VALID_REVISION).isDevVersion(),
                "1.2.0 must not be a dev version");
    }

    private static void assertInvalid(ReleaseRequest request) {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class, request::validate,
                "invalid request must be rejected: " + request);
        assertEquals(ReleaseRejectedException.Reason.INVALID_REQUEST, e.getReason(),
                "invalid request must be reported as INVALID_REQUEST");
    }
}
