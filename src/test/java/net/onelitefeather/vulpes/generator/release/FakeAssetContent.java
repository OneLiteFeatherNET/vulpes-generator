package net.onelitefeather.vulpes.generator.release;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;

/**
 * In-process stand-in for {@code GET /project/{projectId}/asset/{assetId}/content}. It holds the bytes of each asset,
 * records every read, and can be made to fail.
 *
 * @author TheMeinerLP
 */
final class FakeAssetContent implements AssetContentSource {

    /** The bytes of each asset, by asset id. */
    final Map<UUID, byte[]> contents = new HashMap<>();
    /** The asset id of every read, in order. */
    final List<UUID> reads = new ArrayList<>();
    /** When set, every read throws it. */
    RuntimeException failWith;

    @Override
    public byte[] content(UUID projectId, UUID assetId) {
        reads.add(assetId);
        if (failWith != null) {
            throw failWith;
        }
        byte[] bytes = contents.get(assetId);
        if (bytes == null) {
            throw new ReleaseRejectedException(SOURCE_UNAVAILABLE, "backend has no content for asset " + assetId);
        }
        return bytes;
    }
}
