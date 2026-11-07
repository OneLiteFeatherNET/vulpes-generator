package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * The read-only repository credential that the base build of a {@code vulpes-base} checkout receives. Reposilite
 * refuses anonymous reads, so the Gradle build of the branch needs it to resolve its dependencies. It is never the
 * publish credential of {@code vulpes.content.maven}.
 *
 * @param username the read-only user name; blank means no credential is passed to the build
 * @param password the read-only password
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("vulpes.base.maven")
public record BaseBuildCredentials(String username, String password) {

    /**
     * Tells whether a read-only credential is configured.
     *
     * @return {@code true} when the user name is not blank
     */
    public boolean configured() {
        return username != null && !username.isBlank();
    }
}
