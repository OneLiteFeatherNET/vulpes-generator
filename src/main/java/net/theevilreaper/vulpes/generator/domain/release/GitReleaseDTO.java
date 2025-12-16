package net.theevilreaper.vulpes.generator.domain.release;

import java.time.OffsetDateTime;

public class GitReleaseDTO {

    private final String version;
    private final String url;
    private final OffsetDateTime publishedAt;

    private GitReleaseDTO(String version, String url, OffsetDateTime publishedAt) {
        this.version = version;
        this.url = url;
        this.publishedAt = publishedAt;
    }

    public static GitReleaseDTO fromRelease(GitRelease release) {
        return new GitReleaseDTO(
                release.tagName(),
                release.htmlUrl(),
                release.publishedAt()
        );
    }

    public static GitReleaseDTO fromTag(String tag) {
        return new GitReleaseDTO(tag, null, null);
    }

    public static GitReleaseDTO unknown() {
        return new GitReleaseDTO("unknown", null, null);
    }

    public String getVersion() {
        return version;
    }

    public String getUrl() {
        return url;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}

