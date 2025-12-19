package net.theevilreaper.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "The version of the release", example = "1.0.0", requiredMode = Schema.RequiredMode.REQUIRED)
        String version,
        @Schema(description = "The url to the release on Github", example = "https://github.com/OneLiteFeatherNET/Vulpes/releases/tag/1.0.0")
        String url,
        @Schema(description = "The date when the release was published", example = "2021-05-20T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
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
