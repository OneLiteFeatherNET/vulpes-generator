package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The release request: the project, the version, the revision of the snapshot and the {@code vulpes-base} branch the
 * release is verified against. The commit is not part of the request; the server resolves it from the branch.
 *
 * @param projectId  the id of the project in the backend
 * @param project    the Maven artifact id of the project
 * @param version    the release version, {@code X.Y.Z} for a release or {@code X.Y.Z-SNAPSHOT} for a snapshot
 * @param revision   the {@code sha256:} digest of the snapshot bytes
 * @param createdBy  the person who requested the release
 * @param createdAt  the moment the release was requested, ISO-8601 in UTC
 * @param baseBranch the {@code vulpes-base} branch, {@code develop} when omitted
 * @author TheMeinerLP
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
        Instant createdAt,
        String baseBranch
) {

    private static final Pattern PROJECT_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-SNAPSHOT)?");
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
        validateVersion(version);
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
     * Checks a version alone, so that a request can be refused before any backend or repository call.
     *
     * @param version the version to check
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#INVALID_REQUEST} when the version is
     *                                  not {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT} without leading zeros
     */
    public static void validateVersion(String version) {
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw invalid("version must be X.Y.Z or X.Y.Z-SNAPSHOT without leading zeros");
        }
    }

    /**
     * Tells whether this request targets a snapshot version such as {@code 1.2.0-SNAPSHOT}.
     *
     * @return {@code true} when the version carries the {@code -SNAPSHOT} suffix
     */
    public boolean isSnapshotVersion() {
        return version.endsWith("-SNAPSHOT");
    }

    /**
     * Returns the request with another base branch, for example the branch resolved from the requested one.
     *
     * @param branch the base branch
     * @return a copy of this request with the given branch
     */
    public ReleaseRequest withBaseBranch(String branch) {
        return new ReleaseRequest(projectId, project, version, revision, createdBy, createdAt, branch);
    }

    private static ReleaseRejectedException invalid(String message) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.INVALID_REQUEST, message);
    }
}
