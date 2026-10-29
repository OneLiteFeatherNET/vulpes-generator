package net.onelitefeather.vulpes.generator.release;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Builds minimal format-1 snapshot bytes and matching requests for the release tests.
 * The snapshot is built in the test itself; no fixture is copied from another repository.
 */
/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
final class ReleaseTestData {

    static final UUID PROJECT_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-8c0a7b6c3d11");
    static final Instant CREATED_AT = Instant.parse("2026-10-28T12:00:00Z");

    private ReleaseTestData() {
    }

    static byte[] snapshot(UUID projectId) {
        return ("{\"format\":1,\"projectId\":\"" + projectId + "\",\"items\":[]}").getBytes(StandardCharsets.UTF_8);
    }

    static String digest(byte[] bytes) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(sha256.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }

    static ReleaseRequest request(String version, String revision) {
        return new ReleaseRequest(PROJECT_ID, "eldoria", version, revision, "theEvilReaper", CREATED_AT);
    }

    static ReleaseRequest request() {
        return request("1.2.0", digest(snapshot(PROJECT_ID)));
    }
}
