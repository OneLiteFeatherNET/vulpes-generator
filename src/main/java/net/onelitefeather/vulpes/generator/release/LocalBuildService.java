package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.base.BaseVerification;
import net.onelitefeather.vulpes.generator.release.base.BaseVerification.VerifiedBase;

import java.time.Clock;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.INVALID_REQUEST;

/**
 * Builds the local build of a project for a developer: the snapshot of the backend, verified against a base branch,
 * packed with its release metadata in release format 2 and the version {@code 0.0.0-SNAPSHOT}. Nothing is published
 * and nothing is written to disk; the zip is built in memory.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class LocalBuildService {

    /** The version of every local build. */
    static final String VERSION = "0.0.0-SNAPSHOT";
    /** The creator written into the provenance of a local build. */
    static final String CREATED_BY = "local";

    private final ProjectSource projects;
    private final BaseBranchResolver branches;
    private final BaseVerification baseVerification;
    private final ReleaseJsonWriter jsonWriter;
    private final String repository;
    private final Clock clock;
    private final String generatorVersion;

    /**
     * Creates the service.
     *
     * @param projects         the backend, for the project key and the snapshot
     * @param branches         resolves the requested base branch
     * @param baseVerification verifies the snapshot against the base branch
     * @param jsonWriter       writes {@code release.json}
     * @param base             the {@code vulpes.base.*} configuration, which names the base repository
     * @param clock            the source of {@code createdAt}
     * @param generatorVersion the version of this generator, written into the provenance
     */
    @Inject
    public LocalBuildService(ProjectSource projects, BaseBranchResolver branches, BaseVerification baseVerification,
                             ReleaseJsonWriter jsonWriter, BaseConfiguration base, Clock clock,
                             @Value("${vulpes.generator.version:dev}") String generatorVersion) {
        this.projects = projects;
        this.branches = branches;
        this.baseVerification = baseVerification;
        this.jsonWriter = jsonWriter;
        this.repository = base.repository();
        this.clock = clock;
        this.generatorVersion = generatorVersion;
    }

    /**
     * Builds the local build of a project against a base branch.
     *
     * @param projectId  the id of the project in the backend
     * @param baseBranch the {@code vulpes-base} branch, {@code develop} when {@code null} or blank
     * @return the project key and the zip
     * @throws ReleaseRejectedException when the project, the branch or the snapshot is refused, or the sources do not
     *                                  compile against the branch
     */
    public LocalBuild build(UUID projectId, String baseBranch) {
        if (projectId == null) {
            throw new ReleaseRejectedException(INVALID_REQUEST, "projectId must not be null");
        }
        String project = projects.projectKey(projectId);
        String branch = branches.resolve(baseBranch);
        AssembledSnapshot snapshot = projects.assemble(projectId);
        ReleaseRequest request = new ReleaseRequest(projectId, project, VERSION,
                SnapshotVerifier.digest(snapshot.bytes()), CREATED_BY, clock.instant(), branch);
        request.validate();
        VerifiedBase verified = baseVerification.verify(new BaseVerification.Input(branch, project,
                GeneratedPackage.of(project), snapshot.bytes(),
                commit -> jsonWriter.write(request, new ReleaseBase(repository, branch, commit), generatorVersion)));
        return new LocalBuild(project, LocalBuildZip.write(project, verified.releaseJson(), snapshot.bytes()));
    }
}
