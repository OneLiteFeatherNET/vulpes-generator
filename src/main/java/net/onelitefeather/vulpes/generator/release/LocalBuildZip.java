package net.onelitefeather.vulpes.generator.release;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Assembles a local build in memory: a zip with exactly {@code vulpes/<project>/release.json} and
 * {@code vulpes/<project>/snapshot.json}, and no other entry. The bytes are not rewritten. The entries carry a fixed
 * time, so the same input always gives the same zip.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LocalBuildZip {

    /** The earliest timestamp a ZIP entry can carry, the same as the content data JAR. */
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(1980, 2, 1, 0, 0);

    private LocalBuildZip() {
    }

    /**
     * Writes the local build zip of a project.
     *
     * @param projectKey  the project key, which names the folder of the entries
     * @param releaseJson the {@code release.json} bytes, stored unchanged
     * @param snapshotJson the snapshot bytes, stored unchanged
     * @return the zip bytes
     */
    public static byte[] write(String projectKey, byte[] releaseJson, byte[] snapshotJson) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            writeEntry(zip, "vulpes/" + projectKey + "/release.json", releaseJson);
            writeEntry(zip, "vulpes/" + projectKey + "/snapshot.json", snapshotJson);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the local build zip", e);
        }
        return out.toByteArray();
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        entry.setTimeLocal(ENTRY_TIME);
        entry.setMethod(ZipEntry.DEFLATED);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }
}
