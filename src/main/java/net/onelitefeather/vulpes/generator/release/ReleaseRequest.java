package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The body of a {@code POST /releases} request.
 *
 * @param projectId the id of the project in the backend
 * @param project   the Maven artifact id of the project
 * @param version   the release version, {@code X.Y.Z} or {@code X.Y.Z-dev.N}
 * @param revision  the {@code sha256:} digest of the snapshot bytes
 * @param createdBy the person who requested the release
 * @param createdAt the moment the release was requested, ISO-8601 in UTC
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseRequest(
        UUID projectId,
        String project,
        String version,
        String revision,
        String createdBy,
        Instant createdAt
) {

    private static final Pattern PROJECT_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-dev\\.(0|[1-9][0-9]*))?");
    private static final Pattern REVISION_PATTERN = Pattern.compile("sha256:[0-9a-f]{64}");

    /**
     * Checks every field against the release request rules.
     *
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#INVALID_REQUEST}
     *                                  when a field is missing or malformed
     */
    public void validate() {
        if (projectId == null) {
            throw invalid("projectId must not be null");
        }
        if (project == null || !PROJECT_PATTERN.matcher(project).matches()) {
            throw invalid("project must match ^[a-z][a-z0-9-]*$");
        }
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw invalid("version must be X.Y.Z or X.Y.Z-dev.N without leading zeros");
        }
        if (revision == null || !REVISION_PATTERN.matcher(revision).matches()) {
            throw invalid("revision must be sha256: followed by 64 lowercase hex digits");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw invalid("createdBy must not be blank");
        }
        if (createdAt == null) {
            throw invalid("createdAt must not be null");
        }
    }

    /**
     * Tells whether this request targets a development version such as {@code 1.2.0-dev.3}.
     *
     * @return {@code true} when the version carries the {@code -dev.N} suffix
     */
    public boolean isDevVersion() {
        return version.contains("-dev.");
    }

    private static ReleaseRejectedException invalid(String message) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.INVALID_REQUEST, message);
    }
}
