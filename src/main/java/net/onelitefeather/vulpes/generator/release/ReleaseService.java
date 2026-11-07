package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.base.BaseVerification;
import net.onelitefeather.vulpes.generator.release.base.BaseVerification.VerifiedBase;

import java.nio.file.Path;

/**
 * Turns one snapshot into a published content data JAR.
 * The steps run in a fixed order: validate, refuse an existing release version before anything is fetched, resolve
 * the base branch, fetch the snapshot, verify it, verify it against the base (clone, cached base classes, codegen,
 * compilation), build the JAR in a temporary workspace, publish, notify.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class ReleaseService {

    private final ReleaseSnapshotSource source;
    private final SnapshotVerifier verifier;
    private final ReleaseJsonWriter jsonWriter;
    private final DataJarBuilder jarBuilder;
    private final ContentPublisher publisher;
    private final ReleaseCompletionListener listener;
    private final BaseBranchResolver branches;
    private final BaseVerification baseVerification;
    private final String repository;
    private final String generatorVersion;
    private final Path workDir;

    /**
     * Creates the service.
     *
     * @param source           the port that provides the frozen snapshot
     * @param verifier         checks the snapshot against the request
     * @param jsonWriter       writes {@code release.json}
     * @param jarBuilder       builds the data JAR
     * @param publisher        the port that publishes the JAR
     * @param listener         notified after a successful publish
     * @param branches         resolves the requested base branch
     * @param baseVerification verifies the snapshot against the base branch
     * @param base             the {@code vulpes.base.*} configuration, which names the base repository
     * @param generatorVersion the version of this generator, written into the provenance
     * @param workDir          the base directory for build workspaces; blank means the JVM temp directory
     */
    public ReleaseService(ReleaseSnapshotSource source,
                          SnapshotVerifier verifier,
                          ReleaseJsonWriter jsonWriter,
                          DataJarBuilder jarBuilder,
                          ContentPublisher publisher,
                          ReleaseCompletionListener listener,
                          BaseBranchResolver branches,
                          BaseVerification baseVerification,
                          BaseConfiguration base,
                          @Value("${vulpes.generator.version:dev}") String generatorVersion,
                          @Value("${vulpes.release.work-dir:}") String workDir) {
        this.source = source;
        this.verifier = verifier;
        this.jsonWriter = jsonWriter;
        this.jarBuilder = jarBuilder;
        this.publisher = publisher;
        this.listener = listener;
        this.branches = branches;
        this.baseVerification = baseVerification;
        this.repository = base.repository();
        this.generatorVersion = generatorVersion;
        this.workDir = workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir);
    }

    /**
     * Publishes the content release described by the request. The snapshot is fetched from the source.
     *
     * @param request the release request; a blank or missing base branch means {@code develop}
     * @return the coordinates, the revision, the verified base and the repository of the published release
     * @throws ReleaseRejectedException when the release is refused; nothing has been published then
     */
    public ReleaseResult release(ReleaseRequest request) {
        request.validate();
        ContentCoordinates coordinates = ContentCoordinates.forRelease(request.project(), request.version());
        refuseExisting(coordinates);
        ReleaseRequest resolved = request.withBaseBranch(branches.resolve(request.baseBranch()));
        byte[] snapshot = source.fetch(request.projectId());
        return publishVerified(resolved, snapshot);
    }

    /**
     * Verifies and publishes a snapshot that the caller has already assembled. The base branch of the request must
     * already be resolved, and the existence of the version must already be checked by the caller.
     *
     * @param request  the release request, whose revision must be the digest of the snapshot
     * @param snapshot the snapshot bytes
     * @return the coordinates, the revision, the verified base and the repository of the published release
     * @throws ReleaseRejectedException when the release is refused; nothing has been published then
     */
    public ReleaseResult publishVerified(ReleaseRequest request, byte[] snapshot) {
        request.validate();
        ContentCoordinates coordinates = ContentCoordinates.forRelease(request.project(), request.version());
        verifier.verify(request, snapshot);
        VerifiedBase verified = baseVerification.verify(new BaseVerification.Input(request.baseBranch(),
                request.project(), GeneratedPackage.of(request.project()), snapshot,
                commit -> jsonWriter.write(request, new ReleaseBase(repository, request.baseBranch(), commit),
                        generatorVersion)));
        ReleaseBase base = new ReleaseBase(repository, verified.branch(), verified.commit());
        try (BuildWorkspace workspace = BuildWorkspace.create(workDir)) {
            Path jarFile = workspace.dir().resolve(coordinates.artifactId() + "-" + coordinates.version() + ".jar");
            jarBuilder.build(jarFile, verified.releaseJson(), snapshot);
            publisher.publish(coordinates, jarFile);
        }
        listener.completed(coordinates, request.revision());
        return new ReleaseResult(coordinates, request.revision(), base, publisher.repositoryUrl(coordinates));
    }

    /**
     * Refuses a release version that already exists. Snapshot versions are never refused: a new upload replaces the
     * previous snapshot of the version.
     */
    private void refuseExisting(ContentCoordinates coordinates) {
        if (!coordinates.isSnapshotVersion() && publisher.exists(coordinates)) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.VERSION_EXISTS,
                    "version " + coordinates.version() + " of " + coordinates.artifactId() + " already exists");
        }
    }
}
