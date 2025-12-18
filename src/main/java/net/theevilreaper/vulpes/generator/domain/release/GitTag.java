package net.theevilreaper.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Simple pojo class to represent a Git Tag
 *
 * @param name the name of the tag
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record GitTag(String name) {
}
