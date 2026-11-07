package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.Clock;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.INVALID_REQUEST;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.VERSION_EXISTS;

/**
 * Starts a release on behalf of the editor. The order is fixed: the version is checked, the project key is read from
 * the backend, an existing release version is refused, the base branch is resolved, and only then the snapshot is
 * assembled and verified against the base. The release runs with the editor as creator.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class EditorReleaseService {

    /** The creator written into the provenance of every release started from the editor. */
    static final String CREATED_BY = "stelaris";

    private final ProjectSource projects;
    private final ContentPublisher publisher;
    private final BaseBranchResolver branches;
    private final ReleaseService releaseService;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param projects       the backend, for the project key and the snapshot
     * @param publisher      checks whether a release version exists
     * @param branches       resolves the requested base branch
     * @param releaseService verifies and publishes the assembled snapshot
     * @param clock          the source of {@code createdAt}
     */
    @Inject
    public EditorReleaseService(ProjectSource projects, ContentPublisher publisher, BaseBranchResolver branches,
                                ReleaseService releaseService, Clock clock) {
        this.projects = projects;
        this.publisher = publisher;
        this.branches = branches;
        this.releaseService = releaseService;
        this.clock = clock;
    }

    /**
     * Publishes the release of a project in the given version against a base branch.
     *
     * @param projectId  the id of the project in the backend
     * @param version    the release version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
     * @param baseBranch the {@code vulpes-base} branch, {@code develop} when {@code null} or blank
     * @return the coordinates, the revision, the verified base and the repository of the published release
     * @throws ReleaseRejectedException when the release is refused; nothing has been published then
     */
    public ReleaseResult release(UUID projectId, String version, String baseBranch) {
        if (projectId == null) {
            throw new ReleaseRejectedException(INVALID_REQUEST, "projectId must not be null");
        }
        ReleaseRequest.validateVersion(version);
        String project = projects.projectKey(projectId);
        ContentCoordinates coordinates = ContentCoordinates.forRelease(project, version);
        if (!coordinates.isSnapshotVersion() && publisher.exists(coordinates)) {
            throw new ReleaseRejectedException(VERSION_EXISTS,
                    "version " + version + " of " + project + " already exists");
        }
        String branch = branches.resolve(baseBranch);
        AssembledSnapshot snapshot = projects.assemble(projectId);
        ReleaseRequest request = new ReleaseRequest(projectId, project, version,
                SnapshotVerifier.digest(snapshot.bytes()), CREATED_BY, clock.instant(), branch);
        return releaseService.publishVerified(request, snapshot.bytes());
    }
}
