package net.onelitefeather.vulpes.generator.release;

/**
 * A format 1 snapshot assembled from the backend, with the key of the project it belongs to and the inputs of its
 * resource pack, which the snapshot itself does not carry.
 *
 * @param projectKey the project key, which is the namespace and the project name of the release
 * @param bytes      the canonical snapshot bytes
 * @param pack       the pack settings and the asset files of the project, {@link ResourcePackInputs#NONE} when unknown
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record AssembledSnapshot(String projectKey, byte[] bytes, ResourcePackInputs pack) {

    /**
     * Creates a snapshot without pack inputs.
     *
     * @param projectKey the project key
     * @param bytes      the canonical snapshot bytes
     */
    public AssembledSnapshot(String projectKey, byte[] bytes) {
        this(projectKey, bytes, ResourcePackInputs.NONE);
    }
}
