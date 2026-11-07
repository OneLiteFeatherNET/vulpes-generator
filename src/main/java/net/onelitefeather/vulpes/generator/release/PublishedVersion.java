package net.onelitefeather.vulpes.generator.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * One published version of a project, as the editor lists it.
 *
 * @param version       the version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
 * @param channel       {@code release} or {@code snapshot}
 * @param coordinates   the Maven coordinates {@code group:artifact:version}
 * @param repositoryUrl the URL of the repository the version lives in
 * @param jarUrl        the URL of the data JAR, the timestamped file for a snapshot
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record PublishedVersion(String version, String channel, String coordinates, String repositoryUrl, String jarUrl) {
}
