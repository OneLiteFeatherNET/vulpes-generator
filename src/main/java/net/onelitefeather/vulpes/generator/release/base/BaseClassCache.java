package net.onelitefeather.vulpes.generator.release.base;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.BuildWorkspace;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Caches the built classes of {@code vulpes-base} per full commit SHA. A cache entry holds the base jar and the
 * dependency jars and nothing else: no clone and no source. A commit is built at most once while its entry exists.
 * Only {@code vulpes.base.cache-entries} commits are kept; the least recently used one is evicted first.
 * <p>
 * The lookup happens before the single build permit is taken, so a warm commit never waits for a cold build of
 * another commit. A build is written into a partial directory of the cache and moved into place only when it has
 * succeeded, so a failed or timed out build leaves no entry.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseClassCache {

    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}");
    private static final String PARTIAL_PREFIX = ".partial-";

    private final BaseBuild builder;
    private final Path cacheDir;
    private final int cacheEntries;
    private final Clock clock;
    private final Semaphore buildPermit = new Semaphore(1);

    /**
     * Creates the cache from the configuration.
     *
     * @param builder the base build port
     * @param base    the {@code vulpes.base.*} configuration with the cache directory and its size
     * @param clock   the source of the times of the uses
     * @param workDir the base directory of the requests; the default cache directory lies in it
     */
    @Inject
    public BaseClassCache(BaseBuild builder, BaseConfiguration base, Clock clock,
                          @Value("${vulpes.release.work-dir:}") String workDir) {
        this(builder, cacheDirOf(base.cacheDir(), workDir), base.cacheEntries(), clock);
    }

    /**
     * Creates the cache for explicit values.
     *
     * @param builder      the base build port
     * @param cacheDir     the directory of the cache entries
     * @param cacheEntries the number of commits kept
     * @param clock        the source of the times of the uses
     */
    BaseClassCache(BaseBuild builder, Path cacheDir, int cacheEntries, Clock clock) {
        if (cacheEntries < 1) {
            throw new IllegalArgumentException("the base cache must keep at least one commit");
        }
        this.builder = builder;
        this.cacheDir = cacheDir;
        this.cacheEntries = cacheEntries;
        this.clock = clock;
    }

    private static Path cacheDirOf(String configured, String workDir) {
        if (!configured.isBlank()) {
            return Path.of(configured);
        }
        Path base = workDir.isBlank() ? Path.of(System.getProperty("java.io.tmpdir")) : Path.of(workDir);
        return base.resolve("base-cache");
    }

    /**
     * Returns the classes of a commit, building them from the checkout when the cache has no entry for it.
     *
     * @param commit   the full 40-character lower-case SHA-1 of the commit
     * @param checkout the checkout of that commit, used only when the commit must be built
     * @return the classes of the commit
     * @throws ReleaseRejectedException with {@code BASE_UNAVAILABLE} when the base build fails or times out
     */
    public BaseClasses resolve(String commit, Path checkout) {
        if (!COMMIT.matcher(commit).matches()) {
            throw new IllegalArgumentException("commit must be a full 40-character lower-case SHA-1: " + commit);
        }
        Path entry = cacheDir.resolve(commit);
        if (isEntry(entry)) {
            return use(entry);
        }
        buildPermit.acquireUninterruptibly();
        try {
            if (isEntry(entry)) {
                return use(entry);
            }
            return build(commit, checkout, entry);
        } finally {
            buildPermit.release();
        }
    }

    private BaseClasses build(String commit, Path checkout, Path entry) {
        createDirectories(cacheDir);
        deletePartialDirectories();
        Path partial = cacheDir.resolve(PARTIAL_PREFIX + commit);
        try {
            createDirectories(partial);
            builder.build(checkout, partial);
            Files.move(partial, entry, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot store the base build of " + commit, e);
        } finally {
            BuildWorkspace.deleteTree(partial);
        }
        touch(entry);
        evict(entry);
        return BaseClasses.of(entry);
    }

    private BaseClasses use(Path entry) {
        touch(entry);
        return BaseClasses.of(entry);
    }

    private boolean isEntry(Path entry) {
        return Files.isRegularFile(entry.resolve(BaseBuildRunner.JAR_NAME));
    }

    private void touch(Path entry) {
        try {
            Files.setLastModifiedTime(entry, FileTime.from(clock.instant()));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot mark the base cache entry " + entry + " as used", e);
        }
    }

    private void evict(Path stored) {
        List<Path> entries = commitEntries();
        int excess = entries.size() - cacheEntries;
        for (Path entry : entries) {
            if (excess <= 0) {
                return;
            }
            if (!entry.equals(stored)) {
                BuildWorkspace.deleteTree(entry);
                excess--;
            }
        }
    }

    private List<Path> commitEntries() {
        try (Stream<Path> children = Files.list(cacheDir)) {
            return children
                    .filter(path -> COMMIT.matcher(path.getFileName().toString()).matches())
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(BaseClassCache::lastUse))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot list the base cache " + cacheDir, e);
        }
    }

    private void deletePartialDirectories() {
        try (Stream<Path> children = Files.list(cacheDir)) {
            children.filter(path -> path.getFileName().toString().startsWith(PARTIAL_PREFIX))
                    .forEach(BuildWorkspace::deleteTree);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot clean the base cache " + cacheDir, e);
        }
    }

    private static FileTime lastUse(Path entry) {
        try {
            return Files.getLastModifiedTime(entry);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the last use of " + entry, e);
        }
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create " + dir, e);
        }
    }
}
