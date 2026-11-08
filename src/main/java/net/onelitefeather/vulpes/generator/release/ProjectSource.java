package net.onelitefeather.vulpes.generator.release;

import java.util.UUID;

/**
 * Port to the backend for the identity of a project and for its snapshot. The project key is the Maven artifact id
 * of every release of the project, so it is known before the snapshot is assembled.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ProjectSource {

    /**
     * Returns the project key of a project.
     *
     * @param projectId the id of the project in the backend
     * @return the project key, which matches {@code ^[a-z][a-z0-9-]*$}
     * @throws ReleaseRejectedException with {@code UNKNOWN_PROJECT} when the backend does not know the project,
     *                                  {@code UNSUPPORTED_CONTENT} when the key is not a valid project key, or
     *                                  {@code SOURCE_UNAVAILABLE} when the backend cannot be reached
     */
    String projectKey(UUID projectId);

    /**
     * Assembles the snapshot of a project, with its key.
     *
     * @param projectId the id of the project in the backend
     * @return the snapshot, with the key of the project as the backend holds it in the content export
     * @throws ReleaseRejectedException when the snapshot cannot be assembled
     */
    AssembledSnapshot assemble(UUID projectId);
}
