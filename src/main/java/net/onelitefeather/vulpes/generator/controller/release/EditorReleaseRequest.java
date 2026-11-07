package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.serde.annotation.Serdeable;

import java.util.UUID;

/**
 * The body of {@code POST /generator/release}, sent by the editor.
 *
 * @param projectId  the id of the project in the backend
 * @param version    the release version, {@code X.Y.Z} or {@code X.Y.Z-SNAPSHOT}
 * @param baseBranch the {@code vulpes-base} branch, {@code develop} when omitted
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Serdeable
public record EditorReleaseRequest(UUID projectId, String version, String baseBranch) {
}
