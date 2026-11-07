package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the startup sweep removes stale request directories and nothing else. The clock is fixed and the
 * directories live in a temporary directory.
 *
 * @author TheMeinerLP
 */
class WorkDirectorySweepTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");

    @TempDir
    Path workDir;

    @Test
    void removesVulpesDirectoryOlderThanOneHour() throws IOException {
        Path stale = directoryModifiedAt("vulpes-release-stale", NOW.minus(Duration.ofHours(2)));

        sweep();

        assertFalse(Files.exists(stale), "a vulpes-* directory older than one hour must be removed");
    }

    @Test
    void keepsVulpesDirectoryYoungerThanOneHour() throws IOException {
        Path fresh = directoryModifiedAt("vulpes-release-fresh", NOW.minus(Duration.ofMinutes(10)));

        sweep();

        assertTrue(Files.exists(fresh), "a vulpes-* directory younger than one hour must be kept");
    }

    @Test
    void removesStaleDirectoryWithContentRecursively() throws IOException {
        Path stale = Files.createDirectories(workDir.resolve("vulpes-base-stale"));
        Path nested = Files.createDirectories(stale.resolve("clone").resolve(".git"));
        Files.writeString(nested.resolve("HEAD"), "ref", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(stale, java.nio.file.attribute.FileTime.from(NOW.minus(Duration.ofHours(3))));

        sweep();

        assertFalse(Files.exists(stale), "a stale directory with content must be removed with its content");
    }

    @Test
    void leavesOtherNamesAloneEvenWhenTheyAreOld() throws IOException {
        Path other = directoryModifiedAt("base-cache", NOW.minus(Duration.ofDays(5)));

        sweep();

        assertTrue(Files.exists(other), "directories without the vulpes- prefix must not be touched");
    }

    @Test
    void leavesOldVulpesFilesAloneBecauseOnlyDirectoriesAreRequestWorkspaces() throws IOException {
        Path file = workDir.resolve("vulpes-notes.txt");
        Files.writeString(file, "keep", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.from(NOW.minus(Duration.ofHours(4))));

        sweep();

        assertTrue(Files.exists(file), "a regular file with the vulpes- prefix must not be deleted");
    }

    @Test
    void missingWorkDirectoryIsNotAnError() {
        Path missing = workDir.resolve("does-not-exist");

        new WorkDirectorySweep(missing.toString(), fixedClock()).sweep();

        assertFalse(Files.exists(missing), "a missing work directory must stay missing");
    }

    private void sweep() {
        new WorkDirectorySweep(workDir.toString(), fixedClock()).sweep();
    }

    private Path directoryModifiedAt(String name, Instant modified) throws IOException {
        Path dir = Files.createDirectories(workDir.resolve(name));
        Files.setLastModifiedTime(dir, java.nio.file.attribute.FileTime.from(modified));
        return dir;
    }

    private static Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
