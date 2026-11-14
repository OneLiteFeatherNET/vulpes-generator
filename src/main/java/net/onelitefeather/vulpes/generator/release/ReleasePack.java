package net.onelitefeather.vulpes.generator.release;

/**
 * A resource pack as {@code release.json} lists it under {@code packs}.
 *
 * @param id         the id of the pack, which is the project key
 * @param resolution the texture resolution of the pack
 * @param url        the public URL of the ZIP
 * @param sha1       the lower-case hex SHA-1 of the ZIP
 * @param size       the size of the ZIP in bytes
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.0.0
 */
public record ReleasePack(String id, int resolution, String url, String sha1, long size) {
}
