package net.theevilreaper.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;
import net.theevilreaper.vulpes.generator.domain.client.GithubClient;

/**
 * Configuration which is required to communicate with Github to retrieve release information
 * over the {@link GithubClient}.
 *
 * @param owner the owner / organization of the repository
 * @param repo  the repository name
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("github")
public record GithubConfiguration(
        String owner,
        String repo
) {
}
