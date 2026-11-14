package net.onelitefeather.vulpes.generator.release;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason.PACK_STORAGE_UNAVAILABLE;

/**
 * In-process stand-in for the S3-compatible pack bucket. It keeps the objects in a map, records every upload in a
 * shared event log, and can be made to refuse uploads.
 *
 * @author TheMeinerLP
 */
final class InMemoryPackStorage implements PackStorage {

    private static final String PUBLIC_BASE = "https://packs.test/";

    private final Map<String, byte[]> objects = new HashMap<>();
    private final List<String> events;
    /** When set, every upload is refused as an unavailable bucket. */
    boolean failPuts;
    /** The number of uploads that reached the bucket. */
    int putCount;

    /**
     * Creates an empty bucket.
     *
     * @param events the shared event log, to which every upload appends {@code put <key>}
     */
    InMemoryPackStorage(List<String> events) {
        this.events = events;
    }

    @Override
    public String url(String key) {
        return PUBLIC_BASE + key;
    }

    @Override
    public boolean exists(String key) {
        return objects.containsKey(key);
    }

    @Override
    public void put(String key, byte[] bytes) {
        if (failPuts) {
            throw new ReleaseRejectedException(PACK_STORAGE_UNAVAILABLE, "the pack bucket refused " + key);
        }
        putCount++;
        objects.put(key, bytes);
        events.add("put " + key);
    }

    /** Stores an object without recording an upload, for a key that already exists. */
    void seed(String key, byte[] bytes) {
        objects.put(key, bytes);
    }

    /** Returns the object stored under the key, or {@code null}. */
    byte[] get(String key) {
        return objects.get(key);
    }
}
