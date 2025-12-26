package net.onelitefeather.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

/**
 * The {@link GitReleaseDTO} is a data transfer object which is sent via API to inform clients about the new release
 *
 * @param version      the version of the release
 * @param url          the url to the release on Github
 * @param publishedAt  the date when the release was published
 * @param prerelease   whether the release is a prerelease or not
 * @param targetCommit targetCommit the commit hash of the release or branch
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
        OffsetDateTime publishedAt,
        @Schema(description = "Whether the release is a prerelease or not", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        boolean prerelease,
        @Schema(description = "The commit hash of the release or branch", example = "master", requiredMode = Schema.RequiredMode.REQUIRED)
        // The naming of this comes from the official api https://docs.github.com/en/rest/releases/releases. Search for target_commitish
        String targetCommitish
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
                release.publishedAt(),
                release.prerelease(),
                release.targetCommitish()
        );
    }

    /**
     * Creates a new release dto from a given tag
     *
     * @param tag the tag to create the dto from
     * @return the created {@link GitReleaseDTO} instance
     */
    public static GitReleaseDTO fromTag(String tag) {
        return new GitReleaseDTO(tag, null, null, false, "");
    }

    /**
     * Creates a new unknown release dto
     *
     * @return the created {@link GitReleaseDTO} instance
     */
    public static GitReleaseDTO unknown() {
        return new GitReleaseDTO("unknown", null, null, false, "");
    }
}
