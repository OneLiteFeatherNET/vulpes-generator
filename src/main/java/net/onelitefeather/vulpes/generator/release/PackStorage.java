package net.onelitefeather.vulpes.generator.release;

/**
 * Port to the public bucket that serves the resource packs. Objects are addressed by an immutable key, so an object
 * that exists is never replaced.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
public interface PackStorage {

    /**
     * Returns the public URL of an object.
     *
     * @param key the key of the object
     * @return the URL under which clients download the object
     */
    String url(String key);

    /**
     * Tells whether an object exists under the key.
     *
     * @param key the key of the object
     * @return {@code true} when the object exists
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#PACK_STORAGE_UNAVAILABLE} when the
     *                                  bucket cannot be asked
     */
    boolean exists(String key);

    /**
     * Stores the bytes under the key.
     *
     * @param key   the key of the object
     * @param bytes the bytes to store
     * @throws ReleaseRejectedException with {@link ReleaseRejectedException.Reason#PACK_STORAGE_UNAVAILABLE} when the
     *                                  bucket refuses the object
     */
    void put(String key, byte[] bytes);
}
