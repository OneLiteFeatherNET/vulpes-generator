package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configuration of the Maven repositories the content data JARs are published to.
 * Versions {@code X.Y.Z} go to the releases repository, {@code X.Y.Z-dev.N} to the snapshots repository.
 *
 * @param username     the user name for the repositories, may be blank for anonymous access
 * @param password     the password for the repositories
 * @param releasesUrl  the URL of the releases repository, without a trailing slash
 * @param snapshotsUrl the URL of the snapshots repository, without a trailing slash
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("vulpes.content.maven")
public record MavenContentConfiguration(
        String username,
        String password,
        String releasesUrl,
        String snapshotsUrl
) {
}
