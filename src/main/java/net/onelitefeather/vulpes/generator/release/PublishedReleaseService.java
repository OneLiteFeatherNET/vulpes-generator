package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.MavenContentConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lists the versions of a project that the releases and the snapshots repositories hold. The versions come from the
 * {@code maven-metadata.xml} of each repository; the newest version of a channel is chosen by its numbers, never by the
 * upload order that the {@code <latest>} and {@code <release>} tags follow.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class PublishedReleaseService {

    private static final String GROUP_PATH = ContentCoordinates.GROUP_ID.replace('.', '/');
    private static final String METADATA = "maven-metadata.xml";

    private final ProjectSource projects;
    private final MavenRepositoryClient repositories;
    private final String releasesUrl;
    private final String snapshotsUrl;

    /**
     * Creates the service.
     *
     * @param projects     the backend, for the project key
     * @param repositories reads the metadata of the repositories
     * @param maven        the repository URLs
     */
    @Inject
    public PublishedReleaseService(ProjectSource projects, MavenRepositoryClient repositories,
                                   MavenContentConfiguration maven) {
        this.projects = projects;
        this.repositories = repositories;
        this.releasesUrl = maven.releasesUrl();
        this.snapshotsUrl = maven.snapshotsUrl();
    }

    /**
     * Lists the published versions of a project.
     *
     * @param projectId the id of the project in the backend
     * @return the versions and the latest version of each channel
     * @throws ReleaseRejectedException when the project is unknown, its key is invalid or a repository cannot be read
     */
    public PublishedVersions list(UUID projectId) {
        String project = projects.projectKey(projectId);
        List<String> releases = versions(releasesUrl, MavenRepositoryClient.RELEASES, project).stream()
                .filter(version -> ReleaseVersions.channel(version).equals(ReleaseVersions.RELEASE))
                .sorted(ReleaseVersions::compare)
                .toList();
        List<String> snapshots = versions(snapshotsUrl, MavenRepositoryClient.SNAPSHOTS, project).stream()
                .filter(version -> ReleaseVersions.channel(version).equals(ReleaseVersions.SNAPSHOT))
                .sorted(ReleaseVersions::compare)
                .toList();

        List<PublishedVersion> entries = new ArrayList<>();
        for (String release : releases) {
            entries.add(entry(project, release, releasesUrl, release));
        }
        for (String snapshot : snapshots) {
            String file = jarFileVersion(snapshotsUrl, MavenRepositoryClient.SNAPSHOTS, project, snapshot);
            entries.add(entry(project, snapshot, snapshotsUrl, file));
        }
        return new PublishedVersions(new PublishedVersions.Latest(newest(releases), newest(snapshots)), entries);
    }

    /**
     * Returns the file version of the data JAR of a version: the plain version for a release, and for a snapshot the
     * timestamped version that Maven Resolver deployed, or the plain version when the metadata names none.
     *
     * @param repositoryUrl the repository of the version
     * @param name          the repository name in messages
     * @param project       the project key
     * @param version       the version
     * @return the version that the file name of the JAR carries
     */
    String jarFileVersion(String repositoryUrl, String name, String project, String version) {
        if (ReleaseVersions.channel(version).equals(ReleaseVersions.RELEASE)) {
            return version;
        }
        Optional<byte[]> metadata = repositories.read(repositoryUrl, name,
                GROUP_PATH + "/" + project + "/" + version + "/" + METADATA);
        if (metadata.isEmpty()) {
            return version;
        }
        return MavenMetadata.timestampedJarVersion(metadata.get(), describe(name, repositoryUrl))
                .orElse(version);
    }

    private List<String> versions(String repositoryUrl, String name, String project) {
        Optional<byte[]> metadata = repositories.read(repositoryUrl, name,
                GROUP_PATH + "/" + project + "/" + METADATA);
        if (metadata.isEmpty()) {
            return List.of();
        }
        return MavenMetadata.versions(metadata.get(), describe(name, repositoryUrl)).stream()
                .filter(ReleaseVersions::isValid)
                .distinct()
                .toList();
    }

    private PublishedVersion entry(String project, String version, String repositoryUrl, String fileVersion) {
        String jarUrl = repositoryUrl + "/" + GROUP_PATH + "/" + project + "/" + version + "/"
                + project + "-" + fileVersion + ".jar";
        return new PublishedVersion(version, ReleaseVersions.channel(version),
                ContentCoordinates.forRelease(project, version).toString(), repositoryUrl, jarUrl);
    }

    private static String newest(List<String> versions) {
        return versions.stream().max(ReleaseVersions::compare).orElse(null);
    }

    private static String describe(String name, String repositoryUrl) {
        return "the " + name + " repository " + repositoryUrl;
    }
}
