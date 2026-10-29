package net.onelitefeather.vulpes.generator.release;

import jakarta.inject.Singleton;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds the content data JAR. The JAR is reproducible: the entries are written in a fixed order,
 * all with the same local timestamp (independent of the default time zone) and without extra fields
 * or comments, so the same input always yields the same bytes.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class DataJarBuilder {

    static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    static final String RELEASE_JSON_NAME = "META-INF/vulpes/release.json";
    static final String SNAPSHOT_JSON_NAME = "META-INF/vulpes/snapshot.json";

    private static final byte[] MANIFEST = "Manifest-Version: 1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
    /** The earliest timestamp a ZIP entry can carry. Local time, so it never depends on the default time zone. */
    private static final LocalDateTime ENTRY_TIME = LocalDateTime.of(1980, 2, 1, 0, 0);

    /**
     * Writes the JAR to the given file. An existing file is replaced.
     *
     * @param jarFile      the target file, usually inside a build workspace
     * @param releaseJson  the content of {@code META-INF/vulpes/release.json}
     * @param snapshotJson the snapshot bytes, stored unchanged as {@code META-INF/vulpes/snapshot.json}
     * @throws UncheckedIOException when the file cannot be written
     */
    public void build(Path jarFile, byte[] releaseJson, byte[] snapshotJson) {
        try (OutputStream out = Files.newOutputStream(jarFile);
             ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            writeEntry(zip, MANIFEST_NAME, MANIFEST);
            writeEntry(zip, RELEASE_JSON_NAME, releaseJson);
            writeEntry(zip, SNAPSHOT_JSON_NAME, snapshotJson);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write content data jar " + jarFile, e);
        }
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
