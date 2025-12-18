package net.theevilreaper.vulpes.generator.domain.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.OffsetDateTime;

/**
 * The class represents a Github Release pojo object.
 *
 * @param tagName     the tag name of the release
 * @param name        the name of the release
 * @param htmlUrl     the url to the release on github
 * @param publishedAt the date when the release was published
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record GitRelease(
        @JsonProperty("tag_name")
        String tagName,
        String name,
        @JsonProperty("html_url")
        String htmlUrl,
        @JsonProperty("published_at")
        OffsetDateTime publishedAt
) {

}
