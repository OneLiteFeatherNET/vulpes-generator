package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

/**
 * What the content export holds for the resource pack of a project: the pack settings and the files that the backend
 * stores as assets. A pack is built only when the settings are present.
 *
 * @param meta   the resource pack entry of the export (description, formats, enabled features, icon), {@code null}
 *               when the project has none
 * @param assets the files of the pack that the backend holds as assets, in the order of the export, the icon last
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
public record ResourcePackInputs(JsonNode meta, List<PackAsset> assets) {

    /** The inputs of a project without any pack data. */
    public static final ResourcePackInputs NONE = new ResourcePackInputs(null, List.of());

    /**
     * Copies the asset list.
     */
    public ResourcePackInputs {
        assets = List.copyOf(assets);
    }

    /**
     * A file of the pack that the backend holds as an asset.
     *
     * @param path    the path in the pack, for example {@code assets/cygnus/textures/font/glitch.png}
     * @param assetId the id of the asset, which names the bytes to read
     * @param sha1    the SHA-1 of the bytes as the export lists it
     * @param size    the size of the bytes in bytes as the export lists it
     */
    public record PackAsset(String path, UUID assetId, String sha1, long size) {
    }
}
