package net.onelitefeather.vulpes.generator.release;

import java.nio.file.Path;

/**
 * Port that publishes a content data JAR to a Maven repository.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ContentPublisher {

    /**
     * Tells whether the version of the given coordinates already exists in the target repository.
     *
     * @param coordinates the coordinates to check
     * @return {@code true} when the version exists
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#REPOSITORY_UNAVAILABLE}
     *                                  when the repository cannot be reached
     */
    boolean exists(ContentCoordinates coordinates);

    /**
     * Publishes the JAR together with its POM, metadata and checksums. An existing version is never overwritten.
     *
     * @param coordinates the coordinates to publish under
     * @param jarFile     the data JAR to publish
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#REPOSITORY_UNAVAILABLE}
     *                                  when the upload fails
     */
    void publish(ContentCoordinates coordinates, Path jarFile);
}
