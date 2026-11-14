package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.UUID;

/**
 * Checks the current content of a project for the editor. The snapshot is assembled from the backend and checked; no
 * base is verified, nothing is compiled and nothing is published.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class ContentValidationService {

    private final ProjectSource projects;

    /**
     * Creates the service.
     *
     * @param projects the backend, for the snapshot of the project
     */
    @Inject
    public ContentValidationService(ProjectSource projects) {
        this.projects = projects;
    }

    /**
     * Returns the content problems of a project.
     *
     * @param projectId the id of the project in the backend
     * @return every problem of the current content, empty when the content is valid
     * @throws ReleaseRejectedException with {@code UNKNOWN_PROJECT}, {@code SOURCE_UNAVAILABLE} or
     *                                  {@code UNSUPPORTED_CONTENT} when the snapshot cannot be assembled
     */
    public List<ReleaseDiagnostic> validate(UUID projectId) {
        try {
            return ContentValidator.validate(projects.assemble(projectId).bytes());
        } catch (ReleaseRejectedException e) {
            throw e.forProject(projectId);
        }
    }
}
