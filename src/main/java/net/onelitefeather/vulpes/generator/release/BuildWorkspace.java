package net.onelitefeather.vulpes.generator.release;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A fresh, empty temporary directory for one build. Closing the workspace deletes the directory
 * and everything in it, so it can be used in a try-with-resources block that also covers failures.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public final class BuildWorkspace implements AutoCloseable {

    private final Path dir;

    private BuildWorkspace(Path dir) {
        this.dir = dir;
    }

    /**
     * Creates a new empty directory inside the given base directory.
     *
     * @param baseDir the directory in which the workspace is created
     * @return the new workspace
     * @throws UncheckedIOException when the directory cannot be created
     */
    public static BuildWorkspace create(Path baseDir) {
        try {
            Files.createDirectories(baseDir);
            return new BuildWorkspace(Files.createTempDirectory(baseDir, "vulpes-release-"));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create build workspace in " + baseDir, e);
        }
    }

    /**
     * Returns the directory of this workspace.
     *
     * @return the workspace directory
     */
    public Path dir() {
        return dir;
    }

    /**
     * Deletes the workspace directory and its content. Calling it again has no effect.
     *
     * @throws UncheckedIOException when a file cannot be deleted
     */
    @Override
    public void close() {
        deleteTree(dir);
    }

    /**
     * Deletes a directory with everything in it. A missing directory is left alone.
     *
     * @param root the directory to delete
     * @throws UncheckedIOException when a file cannot be deleted
     */
    public static void deleteTree(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot delete directory " + root, e);
        }
    }

    /**
     * Deletes one file or empty directory. JGit writes its pack files read-only, and Windows refuses to delete a
     * read-only file, so a refused delete clears the read-only flag and tries once more.
     */
    private static void delete(Path path) throws IOException {
        try {
            Files.deleteIfExists(path);
        } catch (AccessDeniedException e) {
            if (!path.toFile().setWritable(true)) {
                throw e;
            }
            Files.deleteIfExists(path);
        }
    }
}
