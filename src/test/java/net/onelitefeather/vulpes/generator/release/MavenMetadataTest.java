package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the reader of {@code maven-metadata.xml}: the version list, the timestamped snapshot file, and the refusal of a
 * document that cannot be read.
 *
 * @author TheMeinerLP
 */
class MavenMetadataTest {

    private static final String SOURCE = "the snapshots repository https://repo.test/onelitefeather-snapshots";

    private static final String PROJECT_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>net.onelitefeather.vulpes.content</groupId>
              <artifactId>eldoria</artifactId>
              <versioning>
                <latest>1.1.0-SNAPSHOT</latest>
                <release>1.1.0</release>
                <versions>
                  <version>1.0.0</version>
                  <version>1.1.0-SNAPSHOT</version>
                  <version>1.1.0</version>
                </versions>
              </versioning>
            </metadata>
            """;

    private static final String VERSION_METADATA = """
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata>
              <groupId>net.onelitefeather.vulpes.content</groupId>
              <artifactId>eldoria</artifactId>
              <version>1.1.0-SNAPSHOT</version>
              <versioning>
                <snapshot>
                  <timestamp>20261107.221202</timestamp>
                  <buildNumber>3</buildNumber>
                </snapshot>
                <lastUpdated>20261107221202</lastUpdated>
                <snapshotVersions>
                  <snapshotVersion>
                    <extension>pom</extension>
                    <value>1.1.0-20261107.221202-3</value>
                  </snapshotVersion>
                  <snapshotVersion>
                    <extension>jar</extension>
                    <value>1.1.0-20261107.221202-3</value>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
            """;

    @Test
    void readsEveryVersionOfTheProject() {
        assertEquals(List.of("1.0.0", "1.1.0-SNAPSHOT", "1.1.0"),
                MavenMetadata.versions(bytes(PROJECT_METADATA), SOURCE),
                "the version list must be read in the order of the document");
    }

    @Test
    void documentWithoutVersionsGivesAnEmptyList() {
        String empty = "<metadata><versioning><versions/></versioning></metadata>";

        assertTrue(MavenMetadata.versions(bytes(empty), SOURCE).isEmpty(), "no version means an empty list");
    }

    @Test
    void truncatedDocumentIsUnavailableAndNamesTheRepository() {
        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> MavenMetadata.versions(bytes("<metadata><versioning>"), SOURCE));

        assertEquals(ReleaseRejectedException.Reason.REPOSITORY_UNAVAILABLE, e.getReason(),
                "unreadable metadata must be REPOSITORY_UNAVAILABLE");
        assertTrue(e.getMessage().contains("snapshots repository"), "the message must name the repository: " + e.getMessage());
    }

    @Test
    void notXmlAtAllIsUnavailable() {
        assertThrows(ReleaseRejectedException.class, () -> MavenMetadata.versions(bytes("not xml"), SOURCE));
    }

    @Test
    void timestampedJarOfASnapshotIsReadFromSnapshotVersions() {
        Optional<String> value = MavenMetadata.timestampedJarVersion(bytes(VERSION_METADATA), SOURCE);

        assertEquals(Optional.of("1.1.0-20261107.221202-3"), value,
                "the jar of a snapshot is deployed under its timestamped version, not the plain one");
    }

    @Test
    void versionMetadataWithoutSnapshotVersionsGivesNoTimestamp() {
        String plain = "<metadata><versioning><lastUpdated>1</lastUpdated></versioning></metadata>";

        assertFalse(MavenMetadata.timestampedJarVersion(bytes(plain), SOURCE).isPresent(),
                "without snapshotVersions the caller must use the plain file name");
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
