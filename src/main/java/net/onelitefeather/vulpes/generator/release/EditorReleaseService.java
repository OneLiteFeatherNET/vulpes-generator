package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.git.BaseCheckout;

import java.time.Clock;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.INVALID_REQUEST;

/**
 * Starts a release on behalf of the editor: the snapshot is assembled from the backend, the revision is computed from
 * it, and the release runs with the editor as creator. The project name of the release is the project key.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class EditorReleaseService {

    /** The creator written into the provenance of every release started from the editor. */
    static final String CREATED_BY = "stelaris";

    private final BackendApiReleaseSnapshotSource source;
    private final ReleaseService releaseService;
    private final Clock clock;
    private final BaseCheckout baseCheckout;
    private final BaseConfiguration base;

    /**
     * Creates the service.
     *
     * @param source         assembles the snapshot from the backend
     * @param releaseService publishes the release
     * @param clock          the source of {@code createdAt}
     * @param baseCheckout   resolves the commit of the base branch
     * @param base           the {@code vulpes.base.*} configuration, which names the base repository
     */
    @Inject
    public EditorReleaseService(BackendApiReleaseSnapshotSource source, ReleaseService releaseService, Clock clock,
                                BaseCheckout baseCheckout, BaseConfiguration base) {
        this.source = source;
        this.releaseService = releaseService;
        this.clock = clock;
        this.baseCheckout = baseCheckout;
        this.base = base;
    }

    /**
     * Publishes the release of a project in the given version.
     *
     * @param projectId the id of the project in the backend
     * @param version   the release version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
     * @return the coordinates and the revision of the published release
     * @throws ReleaseRejectedException when the release is refused; nothing has been published then
     */
    public ReleaseResult release(UUID projectId, String version) {
        if (projectId == null) {
            throw new ReleaseRejectedException(INVALID_REQUEST, "projectId must not be null");
        }
        AssembledSnapshot snapshot = source.assemble(projectId);
        String baseBranch = BaseBranchResolver.DEFAULT_BRANCH;
        String baseCommit = baseCheckout.resolveCommit(baseBranch);
        ReleaseRequest request = new ReleaseRequest(projectId, snapshot.projectKey(), version,
                SnapshotVerifier.digest(snapshot.bytes()), CREATED_BY, clock.instant(),
                base.repository(), baseBranch, baseCommit);
        return releaseService.releaseSnapshot(request, snapshot.bytes());
    }
}
