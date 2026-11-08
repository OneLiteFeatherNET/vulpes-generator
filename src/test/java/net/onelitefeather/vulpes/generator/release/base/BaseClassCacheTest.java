package net.onelitefeather.vulpes.generator.release.base;

import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the base build cache with a fake base build. Commits are 40-character names of directories in a temporary
 * cache directory; the clock is moved by the test.
 *
 * @author TheMeinerLP
 */
class BaseClassCacheTest {

    private static final String FIRST = "1111111111111111111111111111111111111111";
    private static final String SECOND = "2222222222222222222222222222222222222222";
    private static final String THIRD = "3333333333333333333333333333333333333333";

    @TempDir
    Path tempDir;

    private final AdvancingClock clock = new AdvancingClock();

    @Test
    void sameCommitIsBuiltOnlyOnce() throws IOException {
        FakeBaseBuild build = new FakeBaseBuild();
        BaseClassCache cache = cache(build, 3);

        cache.resolve(FIRST, checkout("a"));
        cache.resolve(FIRST, checkout("a"));

        assertEquals(1, build.builds(), "the second request for the same commit must reuse the cache");
    }

    @Test
    void cacheEntryHoldsTheJarAndTheClasspathButNoGitOrSources() throws IOException {
        BaseClassCache cache = cache(new FakeBaseBuild(), 3);

        cache.resolve(FIRST, checkout("a"));

        Path entry = tempDir.resolve("cache").resolve(FIRST);
        try (Stream<Path> files = Files.walk(entry)) {
            List<String> names = files.map(path -> entry.relativize(path).toString()).toList();
            assertTrue(names.stream().noneMatch(name -> name.equals(".git") || name.startsWith(".git/")),
                    "the cache entry must hold no .git: " + names);
            assertTrue(names.stream().noneMatch(name -> name.endsWith(".java")),
                    "the cache entry must hold no source file: " + names);
        }
        assertTrue(Files.isRegularFile(entry.resolve("vulpes-base.jar")), "the base jar must be cached");
        assertTrue(Files.isRegularFile(entry.resolve("lib").resolve("minestom.jar")),
                "the dependency jars must be cached");
    }

    @Test
    void resolvedClassesListTheJarFirstAndThenTheDependencies() throws IOException {
        BaseClassCache cache = cache(new FakeBaseBuild(), 3);

        BaseClasses classes = cache.resolve(FIRST, checkout("a"));

        Path entry = tempDir.resolve("cache").resolve(FIRST);
        assertEquals(List.of(entry.resolve("vulpes-base.jar"), entry.resolve("lib").resolve("minestom.jar")),
                classes.classpath(), "the compile classpath is the base jar and every cached dependency jar");
    }

    @Test
    void leastRecentlyUsedCommitIsEvictedBeyondTheConfiguredCount() throws IOException {
        FakeBaseBuild build = new FakeBaseBuild();
        BaseClassCache cache = cache(build, 2);
        cache.resolve(FIRST, checkout("a"));
        clock.advance(Duration.ofMinutes(1));
        cache.resolve(SECOND, checkout("b"));
        clock.advance(Duration.ofMinutes(1));
        cache.resolve(FIRST, checkout("a"));
        clock.advance(Duration.ofMinutes(1));

        cache.resolve(THIRD, checkout("c"));

        Path root = tempDir.resolve("cache");
        assertFalse(Files.exists(root.resolve(SECOND)), "the least recently used commit must be evicted");
        assertTrue(Files.exists(root.resolve(FIRST)), "a commit used after the other one must be kept");
        assertTrue(Files.exists(root.resolve(THIRD)), "the stored commit must be kept");
    }

    @Test
    void evictedCommitIsBuiltAgainOnTheNextRequest() throws IOException {
        FakeBaseBuild build = new FakeBaseBuild();
        BaseClassCache cache = cache(build, 2);
        cache.resolve(FIRST, checkout("a"));
        clock.advance(Duration.ofMinutes(1));
        cache.resolve(SECOND, checkout("b"));
        clock.advance(Duration.ofMinutes(1));
        cache.resolve(THIRD, checkout("c"));
        clock.advance(Duration.ofMinutes(1));

        cache.resolve(FIRST, checkout("a"));

        assertEquals(4, build.builds(), "an evicted commit must be built again");
    }

    @Test
    void buildExceedingItsTimeLimitGivesBaseUnavailableAndLeavesNoCacheEntry() throws IOException {
        BaseBuildRunner timingOut = new BaseBuildRunner(
                new FakeProcessRunner(call -> new ProcessRunner.ProcessResult(-1, true)),
                Duration.ofMinutes(10), Map.of("PATH", "/usr/bin"));
        BaseClassCache cache = cache(timingOut, 3);
        Path checkout = checkout("a");
        Files.writeString(checkout.resolve("gradlew"), "#!/bin/sh\n");

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> cache.resolve(FIRST, checkout));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason());
        assertFalse(Files.exists(tempDir.resolve("cache").resolve(FIRST)),
                "a commit whose build timed out must have no cache entry");
        assertNoPartialDirectory();
    }

    @Test
    void failedBuildLeavesNoPartialDirectoryBehind() throws IOException {
        FakeBaseBuild failing = new FakeBaseBuild(checkout -> {
            throw new IllegalStateException("compiler crashed");
        });
        BaseClassCache cache = cache(failing, 3);

        assertThrows(IllegalStateException.class, () -> cache.resolve(FIRST, checkout("a")));

        assertFalse(Files.exists(tempDir.resolve("cache").resolve(FIRST)));
        assertNoPartialDirectory();
    }

    @Test
    void secondColdBuildStartsOnlyAfterTheFirstHasFinished() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        List<String> events = new CopyOnWriteArrayList<>();
        FakeBaseBuild build = new FakeBaseBuild(checkout -> {
            String name = checkout.getFileName().toString();
            events.add("start " + name);
            if (name.equals("a")) {
                firstStarted.countDown();
                releaseFirst.await();
            }
            events.add("end " + name);
        });
        BaseClassCache cache = cache(build, 3);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<BaseClasses> first = pool.submit(() -> cache.resolve(FIRST, checkout("a")));
            firstStarted.await();
            Future<BaseClasses> second = pool.submit(() -> cache.resolve(SECOND, checkout("b")));
            releaseFirst.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdownNow();
        }

        assertEquals(List.of("start a", "end a", "start b", "end b"), events,
                "a second base build must not overlap the first one");
    }

    @Test
    @Timeout(value = 30, unit = java.util.concurrent.TimeUnit.SECONDS)
    void warmCommitIsServedWhileAnotherCommitIsBeingBuilt() throws Exception {
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        FakeBaseBuild build = new FakeBaseBuild(checkout -> {
            if (checkout.getFileName().toString().equals("a")) {
                firstStarted.countDown();
                releaseFirst.await();
            }
        });
        BaseClassCache cache = cache(build, 3);
        cache.resolve(SECOND, checkout("b"));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<BaseClasses> cold = pool.submit(() -> cache.resolve(FIRST, checkout("a")));
            firstStarted.await();

            BaseClasses warm = assertTimeoutPreemptively(Duration.ofSeconds(20),
                    () -> cache.resolve(SECOND, checkout("b")));

            assertEquals(tempDir.resolve("cache").resolve(SECOND).resolve("vulpes-base.jar"), warm.jar());
            releaseFirst.countDown();
            cold.get();
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void cachedReturnsNothingForACommitWithoutEntryAndBuildsNothing() {
        FakeBaseBuild build = new FakeBaseBuild();
        BaseClassCache cache = cache(build, 3);

        assertTrue(cache.cached(FIRST).isEmpty(), "a commit without an entry must not be reported as cached");
        assertEquals(0, build.builds(), "a lookup must never build the classes of a commit");
    }

    @Test
    void cachedReturnsTheClassesOfAStoredCommitWithoutBuilding() throws IOException {
        FakeBaseBuild build = new FakeBaseBuild();
        BaseClassCache cache = cache(build, 3);
        cache.resolve(FIRST, checkout("a"));

        BaseClasses classes = cache.cached(FIRST).orElseThrow();

        assertEquals(tempDir.resolve("cache").resolve(FIRST).resolve("vulpes-base.jar"), classes.jar(),
                "the cached classes must be the jar of the stored entry");
        assertEquals(1, build.builds(), "a cached commit must not be built again");
    }

    @Test
    void cachedMarksTheEntryAsUsedSoItSurvivesEviction() throws IOException {
        BaseClassCache cache = cache(new FakeBaseBuild(), 2);
        cache.resolve(FIRST, checkout("a"));
        clock.advance(Duration.ofMinutes(1));
        cache.resolve(SECOND, checkout("b"));
        clock.advance(Duration.ofMinutes(1));
        cache.cached(FIRST);
        clock.advance(Duration.ofMinutes(1));

        cache.resolve(THIRD, checkout("c"));

        Path root = tempDir.resolve("cache");
        assertTrue(Files.exists(root.resolve(FIRST)), "an entry looked up by cached() must count as used");
        assertFalse(Files.exists(root.resolve(SECOND)), "the entry not used since the others must be evicted");
    }

    private BaseClassCache cache(BaseBuild build, int entries) {
        return new BaseClassCache(build, tempDir.resolve("cache"), entries, clock);
    }

    private Path checkout(String name) throws IOException {
        return Files.createDirectories(tempDir.resolve("checkouts").resolve(name));
    }

    private void assertNoPartialDirectory() throws IOException {
        try (Stream<Path> entries = Files.list(tempDir.resolve("cache"))) {
            assertTrue(entries.noneMatch(path -> path.getFileName().toString().startsWith(".")),
                    "no partial directory may remain in the cache");
        }
    }
}
