package net.theevilreaper.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration which is required to communicate with Github to retrieve release information
 * over the {@link net.theevilreaper.vulpes.generator.domain.client.GithubBuildClient}.
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
