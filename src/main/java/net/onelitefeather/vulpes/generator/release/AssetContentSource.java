package net.onelitefeather.vulpes.generator.release;

import java.util.UUID;

/**
 * Port to the backend for the bytes of an asset, {@code GET /project/{projectId}/asset/{assetId}/content}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
public interface AssetContentSource {

    /**
     * Reads the bytes of one asset. The caller checks them against the SHA-1 of the export.
     *
     * @param projectId the id of the project in the backend
     * @param assetId   the id of the asset
     * @return the bytes of the asset
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#SOURCE_UNAVAILABLE} when the backend
     *                                  does not answer with the bytes
     */
    byte[] content(UUID projectId, UUID assetId);
}
