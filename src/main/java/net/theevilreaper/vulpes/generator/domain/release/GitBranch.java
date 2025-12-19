package net.theevilreaper.vulpes.generator.domain.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

/**
 * Represents a Git Branch from a repository.
 *
 * @param name        the name of the branch
 * @param isProtected whether the branch is protected or not
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record GitBranch(
        String name,
        @JsonProperty("protected") boolean isProtected
) {
}