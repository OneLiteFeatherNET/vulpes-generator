package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Checks a received snapshot before anything is built from it: the digest must match the requested
 * revision, the snapshot must be format 1 and it must belong to the requested project.
 * The bytes are only read, never re-serialised.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class SnapshotVerifier {

    private static final JsonFactory JSON = new JsonFactory();

    /**
     * Verifies a snapshot against a release request.
     *
     * @param request  the release request
     * @param snapshot the snapshot bytes as received from the backend
     * @throws ReleaseRejectedException {@link ReleaseRejectedException.Reason#REVISION_MISMATCH},
     *                                  {@link ReleaseRejectedException.Reason#UNSUPPORTED_FORMAT} or
     *                                  {@link ReleaseRejectedException.Reason#PROJECT_MISMATCH}
     */
    public void verify(ReleaseRequest request, byte[] snapshot) {
        if (!digest(snapshot).equals(request.revision())) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.REVISION_MISMATCH,
                    "snapshot digest does not match the requested revision");
        }
        TopLevelFields fields = readTopLevelFields(snapshot);
        if (!fields.formatIsOne()) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT,
                    "snapshot format must be 1");
        }
        if (!request.projectId().toString().equals(fields.projectId())) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.PROJECT_MISMATCH,
                    "snapshot belongs to another project");
        }
    }

    private static String digest(byte[] snapshot) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(sha256.digest(snapshot));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by every Java runtime", e);
        }
    }

    /**
     * Streams through the top level of the snapshot object and skips every nested value.
     * A document that is not a well-formed JSON object is reported as an unsupported format.
     */
    private static TopLevelFields readTopLevelFields(byte[] snapshot) {
        boolean formatSeen = false;
        boolean formatIsOne = false;
        boolean projectSeen = false;
        String projectId = null;
        try (JsonParser parser = JSON.createParser(snapshot)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw unsupported();
            }
            while (parser.nextToken() == JsonToken.FIELD_NAME) {
                String name = parser.currentName();
                JsonToken value = parser.nextToken();
                if (value == null) {
                    throw unsupported();
                }
                if ("format".equals(name) && !formatSeen) {
                    formatSeen = true;
                    // Only the integer literal 1 counts, so 1.0, 1e0 and "1" are rejected.
                    formatIsOne = value == JsonToken.VALUE_NUMBER_INT && "1".equals(parser.getText());
                } else if ("projectId".equals(name) && !projectSeen) {
                    projectSeen = true;
                    projectId = value == JsonToken.VALUE_STRING ? parser.getText() : null;
                }
                parser.skipChildren();
            }
            if (parser.currentToken() != JsonToken.END_OBJECT || parser.nextToken() != null) {
                throw unsupported();
            }
        } catch (IOException e) {
            throw unsupported();
        }
        return new TopLevelFields(formatIsOne, projectId);
    }

    private static ReleaseRejectedException unsupported() {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.UNSUPPORTED_FORMAT,
                "snapshot is not a well-formed JSON object");
    }

    private record TopLevelFields(boolean formatIsOne, String projectId) {
    }
}
