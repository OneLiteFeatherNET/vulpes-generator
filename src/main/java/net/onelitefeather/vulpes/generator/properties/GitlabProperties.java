package net.onelitefeather.vulpes.generator.properties;

import io.micronaut.context.annotation.ConfigurationProperties;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Configuration properties for GitLab integration.
 * <p>
 * This class holds various configuration parameters required to interact with GitLab services,
 * such as authentication details, repository URLs, and pipeline-related settings.
 * </p>
 *
 * @param user           The GitLab username used for authentication.
 * @param password       The password used for authentication (if applicable).
 * @param accessToken    The GitLab access token for API interactions.
 * @param gitlabUrl      The base URL of the GitLab instance.
 * @param baseProjectId  The ID of the base GitLab project used in API calls.
 * @param remoteUrl      The remote URL of the Git repository.
 * @param pipelineUrl    The URL for triggering GitLab pipelines.
 * @param deployUrl      The URL for deployment-related actions.
 * @param dependencyUrl  The URL for fetching dependency data.
 * @param branches       The list of branches to be considered; defaults to an empty list if not specified.
 */
@ConfigurationProperties("gitlab")
public record GitlabProperties(
        @NotNull String user,
        @NotNull String password,
        @NotNull String accessToken,
        @NotNull String gitlabUrl,
        int baseProjectId,
        @NotNull String remoteUrl,
        @NotNull String pipelineUrl,
        @NotNull String deployUrl,
        @NotNull String dependencyUrl,
        List<String> branches
) {
    /**
     * Ensures the branches list is never null.
     *
     * @return The list of branches, defaulting to an empty list if null.
     */
    public List<String> branches() {
        return branches != null ? branches : List.of();
    }
}

