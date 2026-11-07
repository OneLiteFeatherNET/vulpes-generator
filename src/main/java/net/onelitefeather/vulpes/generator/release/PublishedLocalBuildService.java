package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.generator.domain.configuration.MavenContentConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.INVALID_REQUEST;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.PUBLISHED_JAR_CORRUPT;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.VERSION_NOT_PUBLISHED;

/**
 * Builds the local build of a published version. The data JAR is read from its repository with the generator's own
 * credentials, its two metadata entries are checked against each other, and they are copied unchanged into the zip of
 * a local build. Nothing is verified against a base: the release was already verified when it was published.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class PublishedLocalBuildService {

    private static final String GROUP_PATH = ContentCoordinates.GROUP_ID.replace('.', '/');

    private final ProjectSource projects;
    private final MavenRepositoryClient repositories;
    private final PublishedReleaseService listing;
    private final MavenContentConfiguration maven;

    /**
     * Creates the service.
     *
     * @param projects     the backend, for the project key
     * @param repositories reads the data JAR from the repository
     * @param listing      resolves the file version of a snapshot
     * @param maven        the repository URLs
     */
    @Inject
    public PublishedLocalBuildService(ProjectSource projects, MavenRepositoryClient repositories,
                                      PublishedReleaseService listing, MavenContentConfiguration maven) {
        this.projects = projects;
        this.repositories = repositories;
        this.listing = listing;
        this.maven = maven;
    }

    /**
     * Builds the local build of a published version.
     *
     * @param projectId the id of the project in the backend
     * @param version   the version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
     * @return the project key and the zip of the local build
     * @throws ReleaseRejectedException with {@code INVALID_REQUEST} for a malformed version (before any call),
     *                                  {@code VERSION_NOT_PUBLISHED} when the version is not in its repository, or
     *                                  {@code PUBLISHED_JAR_CORRUPT} when the JAR does not match its own revision
     */
    public LocalBuild build(UUID projectId, String version) {
        if (!ReleaseVersions.isValid(version)) {
            throw new ReleaseRejectedException(INVALID_REQUEST,
                    "version must be X.Y.Z or X.Y.Z-SNAPSHOT without leading zeros");
        }
        String project = projects.projectKey(projectId);
        boolean snapshotChannel = ReleaseVersions.channel(version).equals(ReleaseVersions.SNAPSHOT);
        String repositoryUrl = snapshotChannel ? maven.snapshotsUrl() : maven.releasesUrl();
        String name = snapshotChannel ? MavenRepositoryClient.SNAPSHOTS : MavenRepositoryClient.RELEASES;
        String fileVersion = listing.jarFileVersion(repositoryUrl, name, project, version);
        String path = GROUP_PATH + "/" + project + "/" + version + "/" + project + "-" + fileVersion + ".jar";
        Optional<byte[]> jar = repositories.read(repositoryUrl, name, path);
        if (jar.isEmpty()) {
            throw new ReleaseRejectedException(VERSION_NOT_PUBLISHED, "version " + version + " of " + project
                    + " is not published in the " + name + " repository " + repositoryUrl);
        }
        byte[] release = entry(jar.get(), DataJarBuilder.RELEASE_JSON_NAME, project, version);
        byte[] snapshot = entry(jar.get(), DataJarBuilder.SNAPSHOT_JSON_NAME, project, version);
        checkIntegrity(project, version, release, snapshot);
        return new LocalBuild(project, LocalBuildZip.write(project, release, snapshot));
    }

    /** The revision of the release must be the digest of the snapshot, which the reader checks as well. */
    private static void checkIntegrity(String project, String version, byte[] release, byte[] snapshot) {
        try {
            ContentReader.readWithoutCrossEntryChecks(project + "-" + version, release, snapshot);
        } catch (ContentFormatException e) {
            throw new ReleaseRejectedException(PUBLISHED_JAR_CORRUPT,
                    "the published data JAR of " + project + " " + version + " is corrupt: " + e.getMessage());
        }
    }

    private static byte[] entry(byte[] jar, String name, String project, String version) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(name)) {
                    return zip.readAllBytes();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the data JAR of " + project + " " + version, e);
        }
        throw new ReleaseRejectedException(PUBLISHED_JAR_CORRUPT,
                "the published data JAR of " + project + " " + version + " has no " + name);
    }
}
