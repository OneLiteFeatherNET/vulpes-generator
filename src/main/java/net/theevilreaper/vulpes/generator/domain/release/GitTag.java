package net.theevilreaper.vulpes.generator.domain.release;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record GitTag(String name) {
}
