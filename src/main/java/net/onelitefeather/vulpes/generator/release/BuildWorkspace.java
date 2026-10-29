package net.onelitefeather.vulpes.generator.release;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A fresh, empty temporary directory for one build. Closing the workspace deletes the directory
 * and everything in it, so it can be used in a try-with-resources block that also covers failures.
 *
 * @author theEvilReaper
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
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot delete build workspace " + dir, e);
        }
    }
}
