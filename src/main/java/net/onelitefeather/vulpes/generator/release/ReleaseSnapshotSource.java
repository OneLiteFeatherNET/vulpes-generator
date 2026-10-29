package net.onelitefeather.vulpes.generator.release;

import java.util.UUID;

/**
 * Port that provides the frozen snapshot of a project. The generator never reads the database;
 * the snapshot is the only input of a release.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ReleaseSnapshotSource {

    /**
     * Fetches the snapshot bytes of a project. The bytes are returned exactly as received.
     *
     * @param projectId the id of the project
     * @return the unchanged snapshot bytes
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#UNKNOWN_PROJECT}
     *                                  when the project does not exist, or
     *                                  {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE}
     *                                  when no snapshot can be obtained
     */
    byte[] fetch(UUID projectId);
}
