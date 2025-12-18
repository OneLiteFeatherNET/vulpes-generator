package net.theevilreaper.vulpes.generator.domain.client;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.domain.configuration.GithubConfiguration;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import net.theevilreaper.vulpes.generator.domain.release.GitRelease;
import net.theevilreaper.vulpes.generator.domain.release.GitTag;

import java.util.List;

@Singleton
public class GithubReleaseService {

    private final GithubBuildClient client;
    private final GithubConfiguration config;

    /**
     * Creates a new instance of the {@link GithubReleaseService}
     *
     * @param client the {@link GithubBuildClient} to use for retrieving the latest release information.
     * @param config the {@link GithubConfiguration} to use for retrieving the repository information.
     */
    @Inject
    public GithubReleaseService(GithubBuildClient client, GithubConfiguration config) {
        this.client = client;
        this.config = config;
    }

    /**
     * Returns the latest release information from the Github repository.
     *
     * @return the latest release information
     */
    public GitReleaseDTO getLatestVersion() {
        try {
            GitRelease release = client.latestRelease(
                    config.owner(),
                    config.repo()
            );

            return GitReleaseDTO.fromRelease(release);
        } catch (HttpClientResponseException _) {
            return fallbackToTags();
        }
    }

    /**
     * Fallback method to get the latest tag if no releases are found
     *
     * @return the latest tag
     */
    private GitReleaseDTO fallbackToTags() {
        List<GitTag> tags = client.tags(config.owner(), config.repo());
        return tags.isEmpty() ? GitReleaseDTO.unknown() : GitReleaseDTO.fromTag(tags.getFirst().name());
    }
}
