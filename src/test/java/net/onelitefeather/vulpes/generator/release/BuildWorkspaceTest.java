package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
class BuildWorkspaceTest {

    @TempDir
    Path baseDir;

    @Test
    void createdWorkspaceIsAnEmptyDirectoryInsideTheBaseDirectory() throws Exception {
        try (BuildWorkspace workspace = BuildWorkspace.create(baseDir)) {
            assertTrue(Files.isDirectory(workspace.dir()), "workspace must be a directory");
            assertEquals(baseDir, workspace.dir().getParent(), "workspace must be created inside the base directory");
            assertEquals(0, entryCount(workspace.dir()), "a fresh workspace must be empty");
        }
    }

    @Test
    void everyWorkspaceIsAFreshDirectory() {
        try (BuildWorkspace first = BuildWorkspace.create(baseDir);
             BuildWorkspace second = BuildWorkspace.create(baseDir)) {
            assertNotEquals(first.dir(), second.dir(), "each build must get its own directory");
        }
    }

    @Test
    void closeDeletesTheWorkspaceWithItsContent() throws Exception {
        BuildWorkspace workspace = BuildWorkspace.create(baseDir);
        Files.createDirectories(workspace.dir().resolve("nested"));
        Files.writeString(workspace.dir().resolve("nested/file.txt"), "content");

        workspace.close();

        assertFalse(Files.exists(workspace.dir()), "closing the workspace must delete it recursively");
    }

    @Test
    void closeLeavesTheBaseDirectoryInPlace() throws Exception {
        BuildWorkspace workspace = BuildWorkspace.create(baseDir);

        workspace.close();

        assertTrue(Files.isDirectory(baseDir), "closing a workspace must not delete its base directory");
    }

    private static long entryCount(Path dir) throws Exception {
        try (Stream<Path> entries = Files.list(Objects.requireNonNull(dir))) {
            return entries.count();
        }
    }

    @Test
    void deleteTreeRemovesReadOnlyFilesLikeGitPacks(@TempDir Path base) throws Exception {
        Path pack = base.resolve("clone/.git/objects/pack/pack-1.pack");
        Files.createDirectories(pack.getParent());
        Files.writeString(pack, "pack");
        assertTrue(pack.toFile().setReadOnly(), "the fixture file must be read-only");

        BuildWorkspace.deleteTree(base.resolve("clone"));

        assertFalse(Files.exists(base.resolve("clone")), "a read-only file must not stop the cleanup");
    }
}
