package net.onelitefeather.vulpes.generator.release;

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Removes the request directories of an earlier generator instance at startup. A killed pod never runs the
 * {@code finally} blocks of its requests, so its {@code vulpes-*} directories would otherwise stay in the work
 * directory. Only directories whose name starts with {@code vulpes-} and whose last change is older than one hour
 * are removed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class WorkDirectorySweep {

    private static final Logger LOG = LoggerFactory.getLogger(WorkDirectorySweep.class);
    private static final String PREFIX = "vulpes-";
    private static final Duration MAX_AGE = Duration.ofHours(1);

    private final Path workDir;
    private final Clock clock;

    /**
     * Creates the sweep.
     *
     * @param workDir the work directory, blank means the JVM temp directory
     * @param clock   the source of the current time
     */
    public WorkDirectorySweep(@Value("${vulpes.release.work-dir:}") String workDir, Clock clock) {
        this.workDir = workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir);
        this.clock = clock;
    }

    /**
     * Runs the sweep once the application has started.
     *
     * @param event the startup event
     */
    @EventListener
    public void onStartup(StartupEvent event) {
        sweep();
    }

    /** Deletes every stale {@code vulpes-*} directory of the work directory. Failures are logged, not thrown. */
    public void sweep() {
        if (!Files.isDirectory(workDir)) {
            return;
        }
        Instant cutoff = clock.instant().minus(MAX_AGE);
        try (Stream<Path> children = Files.list(workDir)) {
            List<Path> candidates = children.toList();
            for (Path child : candidates) {
                if (isStaleRequestDirectory(child, cutoff)) {
                    BuildWorkspace.deleteTree(child);
                    LOG.info("removed stale work directory {}", child.getFileName());
                }
            }
        } catch (IOException | UncheckedIOException e) {
            LOG.warn("cannot sweep the work directory {}: {}", workDir, e.getMessage());
        }
    }

    private static boolean isStaleRequestDirectory(Path child, Instant cutoff) throws IOException {
        Path name = child.getFileName();
        if (name == null || !name.toString().startsWith(PREFIX) || !Files.isDirectory(child)) {
            return false;
        }
        return Files.getLastModifiedTime(child).toInstant().isBefore(cutoff);
    }
}
