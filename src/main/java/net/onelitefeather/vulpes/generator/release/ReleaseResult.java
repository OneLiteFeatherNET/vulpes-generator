package net.onelitefeather.vulpes.generator.release;

/**
 * The outcome of a successful release.
 *
 * @param coordinates the Maven coordinates of the published JAR
 * @param revision    the revision the JAR was built from
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public record ReleaseResult(ContentCoordinates coordinates, String revision) {
}
