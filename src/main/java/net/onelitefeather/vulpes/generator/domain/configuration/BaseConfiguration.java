package net.onelitefeather.vulpes.generator.domain.configuration;

import io.micronaut.context.annotation.ConfigurationProperties;

import java.time.Duration;

/**
 * The limits and the cache of the {@code vulpes-base} checkout that releases and local builds are compiled against.
 * The defaults live in {@code application.yml}.
 *
 * @param repository   the {@code vulpes-base} repository, recorded as {@code base.repository} in {@code release.json}
 * @param cacheDir     the directory of the base build cache; blank means {@code base-cache} in the work directory
 * @param cacheEntries the number of most recently used commits kept in the base build cache
 * @param cloneTimeout the time limit of a shallow clone of a base branch
 * @param buildTimeout the time limit of one Gradle build of a base checkout
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties("vulpes.base")
public record BaseConfiguration(
        String repository,
        String cacheDir,
        int cacheEntries,
        Duration cloneTimeout,
        Duration buildTimeout
) {
}
