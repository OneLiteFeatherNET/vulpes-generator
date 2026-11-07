package net.onelitefeather.vulpes.generator.release;

/**
 * Signals that a release was refused. Nothing has been published when this exception is thrown.
 * The {@link Reason} decides the HTTP status the controller answers with.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public class ReleaseRejectedException extends RuntimeException {

    /**
     * The reason why a release was rejected.
     */
    public enum Reason {
        /** The request body violates the release request rules. */
        INVALID_REQUEST,
        /** The SHA-256 of the snapshot differs from the requested revision. */
        REVISION_MISMATCH,
        /** The snapshot format is not supported or the snapshot could not be parsed. */
        UNSUPPORTED_FORMAT,
        /** The snapshot belongs to another project. */
        PROJECT_MISMATCH,
        /** The backend holds content the format cannot express yet, such as an advancement; nothing is dropped. */
        UNSUPPORTED_CONTENT,
        /** The project changed while the snapshot was assembled and did not settle within the attempts. */
        CONTENT_CHANGED,
        /** The backend does not know the requested project. */
        UNKNOWN_PROJECT,
        /** The requested version already exists in the target repository. */
        VERSION_EXISTS,
        /** The backend answered, but not with a usable snapshot. */
        SOURCE_UNAVAILABLE,
        /** The Maven repository could not be reached or refused the upload. */
        REPOSITORY_UNAVAILABLE,
        /** The requested base branch does not exist in the vulpes-base repository. */
        UNKNOWN_BASE_BRANCH,
        /** The vulpes-base repository, its branch list, its clone or its base build cannot be used. */
        BASE_UNAVAILABLE,
        /** The generated sources do not compile against the base classes of the selected commit. */
        COMPILATION_FAILED,
        /** The code generator refused the snapshot content. */
        GENERATION_REFUSED,
        /** The published content data JAR cannot be read or does not match its revision. */
        PUBLISHED_JAR_CORRUPT
    }

    private final Reason reason;

    /**
     * Creates a new rejection.
     *
     * @param reason  the reason why the release was rejected
     * @param message a human readable description of the rejection
     */
    public ReleaseRejectedException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * Returns the reason why the release was rejected.
     *
     * @return the rejection reason
     */
    public Reason getReason() {
        return reason;
    }
}
