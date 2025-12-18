package net.theevilreaper.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;

import java.time.OffsetDateTime;

/**
 * The {@link GitReleaseDTO} is a data transfer object which is sent via API to inform clients about the new release
 *
 * @param version     the version of the release
 * @param url         the url to the release on Github
 * @param publishedAt the date when the release was published
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record GitReleaseDTO(
        String version,
        String url,
        OffsetDateTime publishedAt
) {

    /**
     * Creates a new release dto from a given release object
     *
     * @param release the release to create the dto from
     * @return the created {@link GitReleaseDTO} instance
     */
    public static GitReleaseDTO fromRelease(GitRelease release) {
        return new GitReleaseDTO(
                release.tagName(),
                release.htmlUrl(),
                release.publishedAt()
        );
    }

    /**
     * Creates a new release dto from a given tag
     *
     * @param tag the tag to create the dto from
     * @return the created {@link GitReleaseDTO} instance
     */
    public static GitReleaseDTO fromTag(String tag) {
        return new GitReleaseDTO(tag, null, null);
    }

    /**
     * Creates a new unknown release dto
     *
     * @return the created {@link GitReleaseDTO} instance
     */
    public static GitReleaseDTO unknown() {
        return new GitReleaseDTO("unknown", null, null);
    }
}
