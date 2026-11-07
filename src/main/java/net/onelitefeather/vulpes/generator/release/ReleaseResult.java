package net.onelitefeather.vulpes.generator.release;

/**
 * The outcome of a successful release.
 *
 * @param coordinates   the Maven coordinates of the published JAR
 * @param revision      the revision the JAR was built from
 * @param base          the {@code vulpes-base} branch and commit the release was verified against
 * @param repositoryUrl the URL of the repository the version was published to
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record ReleaseResult(ContentCoordinates coordinates, String revision, ReleaseBase base, String repositoryUrl) {
}
