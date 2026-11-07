package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

/**
 * The published versions of a project and the newest version of each channel.
 *
 * @param latest   the newest release version and the newest snapshot version, {@code null} for an empty channel
 * @param versions every published version, releases first, each channel in ascending order
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record PublishedVersions(Latest latest, List<PublishedVersion> versions) {

    /**
     * The newest version of each channel.
     *
     * @param release  the newest release version, or {@code null}
     * @param snapshot the newest snapshot version, or {@code null}
     */
    @Serdeable
    public record Latest(String release, String snapshot) {
    }
}
