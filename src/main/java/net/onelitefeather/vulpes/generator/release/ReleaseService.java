package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;

import java.nio.file.Path;

/**
 * Turns one frozen snapshot into a published content data JAR.
 * The steps run in a fixed order: validate, refuse existing versions before anything is fetched,
 * fetch the snapshot, verify it, build the JAR in a temporary workspace, publish, notify.
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
     * @param generatorVersion the version of this generator, written into the provenance
     * @param workDir          the base directory for build workspaces; blank means the JVM temp directory
     */
    public ReleaseService(ReleaseSnapshotSource source,
                          SnapshotVerifier verifier,
                          ReleaseJsonWriter jsonWriter,
                          DataJarBuilder jarBuilder,
                          ContentPublisher publisher,
                          ReleaseCompletionListener listener,
                          @Value("${vulpes.generator.version:dev}") String generatorVersion,
                          @Value("${vulpes.release.work-dir:}") String workDir) {
        this.source = source;
        this.verifier = verifier;
        this.jsonWriter = jsonWriter;
        this.jarBuilder = jarBuilder;
        this.publisher = publisher;
        this.listener = listener;
        this.generatorVersion = generatorVersion;
        this.workDir = workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir);
    }

    /**
     * Publishes the content release described by the request.
     *
     * @param request the release request
     * @return the coordinates and the revision of the published release
     * @throws ReleaseRejectedException when the release is refused; nothing has been published then
     */
    public ReleaseResult release(ReleaseRequest request) {
        request.validate();
        ContentCoordinates coordinates = ContentCoordinates.forRelease(request.project(), request.version());
        if (publisher.exists(coordinates)) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.VERSION_EXISTS,
                    "version " + request.version() + " of " + request.project() + " already exists");
        }
        byte[] snapshot = source.fetch(request.projectId());
        verifier.verify(request, snapshot);

        try (BuildWorkspace workspace = BuildWorkspace.create(workDir)) {
            byte[] releaseJson = jsonWriter.write(request, generatorVersion);
            Path jarFile = workspace.dir().resolve(coordinates.artifactId() + "-" + coordinates.version() + ".jar");
            jarBuilder.build(jarFile, releaseJson, snapshot);
            publisher.publish(coordinates, jarFile);
        }

        listener.completed(coordinates, request.revision());
        return new ReleaseResult(coordinates, request.revision());
    }
}
