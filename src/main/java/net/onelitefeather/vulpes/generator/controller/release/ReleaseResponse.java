package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.serde.annotation.Serdeable;

/**
 * The body of a successful {@code POST /releases}.
 *
 * @param coordinates the Maven coordinates {@code group:artifact:version} of the published JAR
 * @param revision    the revision the JAR was built from
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record ReleaseResponse(String coordinates, String revision) {
}
