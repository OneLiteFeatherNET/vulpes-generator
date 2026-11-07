package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the layout of a local build zip: exactly the two metadata files under {@code vulpes/<project>/}, with the
 * bytes given to the writer.
 *
 * @author TheMeinerLP
 */
class LocalBuildZipTest {

    private static final byte[] RELEASE_JSON = "{\"format\":2}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SNAPSHOT_JSON = "{\"format\":1,\"items\":[]}".getBytes(StandardCharsets.UTF_8);

    @Test
    void zipHoldsExactlyTheReleaseAndTheSnapshotOfTheProject() throws IOException {
        List<String> names = new ArrayList<>(entries(LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON)).keySet());

        assertEquals(List.of("vulpes/eldoria/release.json", "vulpes/eldoria/snapshot.json"), names,
                "the zip must hold exactly the two metadata entries of the project");
    }

    @Test
    void zipHoldsNoGitEntryAndNoJavaSource() throws IOException {
        List<String> names = new ArrayList<>(entries(LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON)).keySet());

        assertTrue(names.stream().noneMatch(name -> name.contains(".git") || name.endsWith(".java")),
                "no git data and no Java source may be in a local build: " + names);
    }

    @Test
    void snapshotEntryIsTheSnapshotBytesUnchanged() throws IOException {
        Map<String, byte[]> entries = entries(LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON));

        assertArrayEquals(SNAPSHOT_JSON, entries.get("vulpes/eldoria/snapshot.json"),
                "snapshot.json must hold the snapshot bytes unchanged");
    }

    @Test
    void releaseEntryIsTheReleaseBytesUnchanged() throws IOException {
        Map<String, byte[]> entries = entries(LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON));

        assertArrayEquals(RELEASE_JSON, entries.get("vulpes/eldoria/release.json"),
                "release.json must hold the release bytes unchanged");
    }

    @Test
    void sameInputGivesTheSameBytes() {
        assertArrayEquals(LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON),
                LocalBuildZip.write("eldoria", RELEASE_JSON, SNAPSHOT_JSON),
                "the zip must not depend on the time it is written");
    }

    private static Map<String, byte[]> entries(byte[] zip) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                assertFalse(entry.isDirectory(), "the zip must not hold directory entries: " + entry.getName());
                entries.put(entry.getName(), in.readAllBytes());
            }
        }
        return entries;
    }
}
