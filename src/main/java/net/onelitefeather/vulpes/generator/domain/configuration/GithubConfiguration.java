package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import net.onelitefeather.vulpes.generator.domain.client.GithubClient;

/**
 * Configuration which is required to communicate with Github to retrieve release information
 * over the {@link GithubClient}.
 *
 * @param owner the owner / organization of the repository
 * @param repo  the repository name
 * @param username the username for authentication
 * @param password the password or token for authentication
 * @param cloneUrl the clone URL of the repository
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 */
@ConfigurationProperties("github")
public record GithubConfiguration(
        String owner,
        String repo,
        String username,
        String password,
        String cloneUrl
) {
}
