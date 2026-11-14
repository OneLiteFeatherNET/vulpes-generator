package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.PACK_STORAGE_UNAVAILABLE;
import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.SOURCE_UNAVAILABLE;

/**
 * Builds the resource pack of a project and publishes it. The bytes of every asset are read from the backend once and
 * checked against the SHA-1 of the export, so a pack never mixes two states of the content. A pack is published only
 * when the pack bucket is configured; the bucket is optional, and without it no pack is prepared.
 * <p>
 * The key of a pack is {@code vulpes/<project>/<sha1>.zip}, so an object is never replaced: a key that exists is
 * not uploaded again.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
@Singleton
public class ResourcePackService {

    /** The texture resolution of the one pack that a release builds. */
    static final int RESOLUTION = 16;

    private final AssetContentSource contents;
    private final Optional<PackStorage> storage;

    /**
     * Creates the service.
     *
     * @param contents the backend, for the bytes of the assets
     * @param storage  the pack bucket, empty when it is not configured
     */
    @Inject
    public ResourcePackService(AssetContentSource contents, Optional<PackStorage> storage) {
        this.contents = contents;
        this.storage = storage;
    }

    /**
     * Builds the ZIP of the resource pack of a project. Nothing is uploaded.
     *
     * @param projectId the id of the project in the backend
     * @param snapshot  the assembled snapshot of the project, with its pack inputs
     * @return the ZIP bytes, or empty when the project has no pack settings
     * @throws ReleaseRejectedException with {@code SOURCE_UNAVAILABLE} when an asset cannot be read or its bytes differ
     *                                  from the export, and the reasons of {@link ResourcePackBuilder}
     */
    public Optional<byte[]> build(UUID projectId, AssembledSnapshot snapshot) {
        ResourcePackInputs inputs = snapshot.pack();
        if (inputs.meta() == null) {
            return Optional.empty();
        }
        Map<UUID, byte[]> bytes = readAssets(projectId, inputs.assets());
        return Optional.of(ResourcePackBuilder.build(snapshot.bytes(), inputs, bytes));
    }

    /**
     * Builds the pack of a release and names its object and its entry for {@code release.json}. Nothing is uploaded;
     * see {@link #upload(PreparedPack)}.
     *
     * @param projectId the id of the project in the backend
     * @param snapshot  the assembled snapshot of the project, with its pack inputs
     * @return the prepared pack, or empty when the bucket is not configured or the project has no pack settings
     * @throws ReleaseRejectedException as {@link #build(UUID, AssembledSnapshot)}
     */
    public Optional<PreparedPack> prepare(UUID projectId, AssembledSnapshot snapshot) {
        if (storage.isEmpty() || snapshot.pack().meta() == null) {
            return Optional.empty();
        }
        byte[] zip = build(projectId, snapshot).orElseThrow();
        String sha1 = sha1(zip);
        String key = "vulpes/" + snapshot.projectKey() + "/" + sha1 + ".zip";
        ReleasePack entry = new ReleasePack(snapshot.projectKey(), RESOLUTION, storage.get().url(key), sha1, zip.length);
        return Optional.of(new PreparedPack(key, zip, entry));
    }

    /**
     * Uploads a prepared pack unless its key already exists in the bucket.
     *
     * @param pack the prepared pack
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#PACK_STORAGE_UNAVAILABLE} when the
     *                                  bucket cannot be used
     */
    public void upload(PreparedPack pack) {
        PackStorage bucket = storage.orElseThrow(() -> new IllegalStateException("no pack storage is configured"));
        if (!bucket.exists(pack.key())) {
            bucket.put(pack.key(), pack.zip());
        }
    }

    /**
     * Reads every asset once and checks its SHA-1 against the export.
     */
    private Map<UUID, byte[]> readAssets(UUID projectId, List<ResourcePackInputs.PackAsset> assets) {
        Map<UUID, byte[]> read = new LinkedHashMap<>();
        for (ResourcePackInputs.PackAsset asset : assets) {
            if (read.containsKey(asset.assetId())) {
                continue;
            }
            byte[] bytes;
            try {
                bytes = contents.content(projectId, asset.assetId());
            } catch (ReleaseRejectedException e) {
                throw new ReleaseRejectedException(SOURCE_UNAVAILABLE,
                        "asset " + asset.path() + " cannot be read: " + e.getMessage());
            }
            if (!sha1(bytes).equals(asset.sha1())) {
                throw new ReleaseRejectedException(SOURCE_UNAVAILABLE, "asset " + asset.path()
                        + " changed while the release was built, its SHA-1 differs from the export");
            }
            read.put(asset.assetId(), bytes);
        }
        return read;
    }

    /** The lower-case hex SHA-1 of the bytes. */
    static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is required by every Java runtime", e);
        }
    }

    /**
     * A pack built for a release, ready to be uploaded.
     *
     * @param key   the key of the object in the bucket
     * @param zip   the ZIP bytes
     * @param entry the entry for {@code release.json}
     */
    public record PreparedPack(String key, byte[] zip, ReleasePack entry) {
    }
}
