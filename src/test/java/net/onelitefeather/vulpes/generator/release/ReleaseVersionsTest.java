package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the numeric order of content versions and the channel of a version.
 *
 * @author TheMeinerLP
 */
class ReleaseVersionsTest {

    @Test
    void tenIsNewerThanNineByItsNumericParts() {
        assertTrue(ReleaseVersions.compare("1.10.0", "1.9.0") > 0, "1.10.0 must be newer than 1.9.0");
    }

    @Test
    void sortingUsesTheNumericParts() {
        List<String> versions = new ArrayList<>(List.of("1.9.0", "1.10.0", "1.2.3"));

        versions.sort(ReleaseVersions::compare);

        assertEquals(List.of("1.2.3", "1.9.0", "1.10.0"), versions, "versions must sort by their numbers");
    }

    @Test
    void snapshotOfAVersionCountsAsThatVersionForOrdering() {
        assertTrue(ReleaseVersions.compare("1.1.0-SNAPSHOT", "1.0.0") > 0, "1.1.0-SNAPSHOT must be newer than 1.0.0");
    }

    @Test
    void channelIsSnapshotForTheSnapshotSuffixAndReleaseOtherwise() {
        assertEquals("snapshot", ReleaseVersions.channel("1.1.0-SNAPSHOT"), "a snapshot suffix is the snapshot channel");
        assertEquals("release", ReleaseVersions.channel("1.1.0"), "a plain version is the release channel");
    }
}
