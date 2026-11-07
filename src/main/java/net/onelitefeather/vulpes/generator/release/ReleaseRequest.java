package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The body of a release request: the project, the version, the revision of the snapshot and the
 * {@code vulpes-base} commit the release is compiled against.
 *
 * @param projectId      the id of the project in the backend
 * @param project        the Maven artifact id of the project
 * @param version        the release version, {@code X.Y.Z} for a release or {@code X.Y.Z-SNAPSHOT} for a snapshot
 * @param revision       the {@code sha256:} digest of the snapshot bytes
 * @param createdBy      the person who requested the release
 * @param createdAt      the moment the release was requested, ISO-8601 in UTC
 * @param baseRepository the {@code vulpes-base} repository, for example {@code OneLiteFeatherNET/vulpes-base}
 * @param baseBranch     the branch the release was compiled against
 * @param baseCommit     the full 40-character lower-case SHA-1 the branch resolved to
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
        String baseRepository,
        String baseBranch,
        String baseCommit
) {

    private static final Pattern PROJECT_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-SNAPSHOT)?");
    private static final Pattern REVISION_PATTERN = Pattern.compile("sha256:[0-9a-f]{64}");
    private static final Pattern COMMIT_PATTERN = Pattern.compile("[0-9a-f]{40}");

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
            throw invalid("version must be X.Y.Z or X.Y.Z-SNAPSHOT without leading zeros");
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
        if (baseRepository == null || baseRepository.isBlank()) {
            throw invalid("baseRepository must not be blank");
        }
        if (baseBranch == null || baseBranch.isBlank()) {
            throw invalid("baseBranch must not be blank");
        }
        if (baseCommit == null || !COMMIT_PATTERN.matcher(baseCommit).matches()) {
            throw invalid("baseCommit must be the full 40 lowercase hex characters of a commit");
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

    private static ReleaseRejectedException invalid(String message) {
        return new ReleaseRejectedException(ReleaseRejectedException.Reason.INVALID_REQUEST, message);
    }
}
