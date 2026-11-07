package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * The {@code vulpes-base} checkout a release was verified against. It is written as the {@code base} object of
 * {@code release.json} and returned to the caller.
 *
 * @param repository the {@code vulpes-base} repository, for example {@code OneLiteFeatherNET/vulpes-base}
 * @param branch     the branch that was verified
 * @param commit     the full 40-character lower-case SHA-1 the branch resolved to
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseBase(String repository, String branch, String commit) {
}
