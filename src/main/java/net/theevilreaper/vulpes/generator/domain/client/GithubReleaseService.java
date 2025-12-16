package net.theevilreaper.vulpes.generator.domain.client;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.domain.configuration.GithubConfiguration;
import net.theevilreaper.vulpes.generator.domain.release.GitReleaseDTO;
import net.theevilreaper.vulpes.generator.domain.release.GitRelease;

@Singleton
public class GithubReleaseService {

    private final GithubBuildClient client;
    private final GithubConfiguration config;

    @Inject
    public GithubReleaseService(GithubBuildClient client, GithubConfiguration config) {
        this.client = client;
        this.config = config;
    }

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

    private GitReleaseDTO fallbackToTags() {
        var tags = client.tags(config.owner(), config.repo());

        if (tags.isEmpty()) {
            return GitReleaseDTO.unknown();
        }

        return GitReleaseDTO.fromTag(tags.getFirst().name());
    }
}
