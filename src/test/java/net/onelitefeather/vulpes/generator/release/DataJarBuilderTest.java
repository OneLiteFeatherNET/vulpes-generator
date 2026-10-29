package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
class DataJarBuilderTest {

    private static final byte[] RELEASE_JSON = "{\"format\":1}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SNAPSHOT_JSON = ReleaseTestData.snapshot(ReleaseTestData.PROJECT_ID);
    private static final List<String> ENTRY_NAMES = List.of(
            "META-INF/MANIFEST.MF",
            "META-INF/vulpes/release.json",
            "META-INF/vulpes/snapshot.json");

    @TempDir
    Path dir;

    private final DataJarBuilder builder = new DataJarBuilder();

    @Test
    void jarContainsExactlyTheThreeEntriesInFixedOrder() throws IOException {
        Path jar = build("first.jar");

        assertEquals(ENTRY_NAMES, entryNames(jar), "entries must be exactly these three, in this order");
    }

    @Test
    void manifestContainsOnlyManifestVersion() throws IOException {
        Path jar = build("first.jar");

        assertArrayEquals("Manifest-Version: 1.0\r\n\r\n".getBytes(StandardCharsets.UTF_8),
                entryBytes(jar, "META-INF/MANIFEST.MF"), "manifest must contain only Manifest-Version: 1.0");
    }

    @Test
    void releaseEntryIsTheGivenReleaseJson() throws IOException {
        Path jar = build("first.jar");

        assertArrayEquals(RELEASE_JSON, entryBytes(jar, "META-INF/vulpes/release.json"),
                "release.json entry must contain the given bytes");
    }

    @Test
    void snapshotEntryIsByteIdenticalToTheSnapshot() throws IOException {
        Path jar = build("first.jar");

        assertArrayEquals(SNAPSHOT_JSON, entryBytes(jar, "META-INF/vulpes/snapshot.json"),
                "snapshot.json entry must be byte-identical to the received snapshot");
    }

    @Test
    void everyEntryHasTheFixedTimestamp() throws IOException {
        Path jar = build("first.jar");

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String name : ENTRY_NAMES) {
                assertEquals(LocalDateTime.of(1980, 2, 1, 0, 0), zip.getEntry(name).getTimeLocal(),
                        "entry " + name + " must carry the fixed timestamp");
            }
        }
    }

    @Test
    void everyEntryIsDeflatedWithoutExtraFieldsOrComment() throws IOException {
        Path jar = build("first.jar");

        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (String name : ENTRY_NAMES) {
                ZipEntry entry = zip.getEntry(name);
                assertEquals(ZipEntry.DEFLATED, entry.getMethod(), "entry " + name + " must be DEFLATED");
                assertTrue(entry.getExtra() == null || entry.getExtra().length == 0,
                        "entry " + name + " must have no extra fields");
                assertTrue(entry.getComment() == null || entry.getComment().isEmpty(),
                        "entry " + name + " must have no comment");
            }
        }
    }

    @Test
    void sameInputYieldsByteIdenticalJars() throws IOException {
        Path first = build("first.jar");
        Path second = build("second.jar");

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second),
                "the same input must produce byte-identical JARs");
    }

    @Test
    void entriesReadBackCarryTheFixedLocalTimeAndNoExtraField() throws IOException {
        Path jar = build("first.jar");

        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                assertEquals(LocalDateTime.of(1980, 2, 1, 0, 0), entry.getTimeLocal(),
                        "entry " + entry.getName() + " must carry the fixed local time");
                assertNull(entry.getExtra(), "entry " + entry.getName() + " must carry no extended timestamp field");
            }
        }
    }

    private Path build(String fileName) {
        Path jar = dir.resolve(fileName);
        builder.build(jar, RELEASE_JSON, SNAPSHOT_JSON);
        return jar;
    }

    private static List<String> entryNames(Path jar) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().getName());
            }
        }
        return names;
    }

    private static byte[] entryBytes(Path jar, String name) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile()); InputStream in = zip.getInputStream(zip.getEntry(name))) {
            return in.readAllBytes();
        }
    }
}
