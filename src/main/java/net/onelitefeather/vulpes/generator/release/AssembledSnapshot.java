package net.onelitefeather.vulpes.generator.release;

/**
 * A format 1 snapshot assembled from the backend, with the key of the project it belongs to.
 *
 * @param projectKey the project key, which is the namespace and the project name of the release
 * @param bytes       the canonical snapshot bytes
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record AssembledSnapshot(String projectKey, byte[] bytes) {
}
