package net.onelitefeather.vulpes.generator.release.base;

import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the real Gradle build of a minimal vulpes-base fixture with the generator's init script. The fixture is copied
 * into a temporary directory together with the Gradle wrapper of this project. Needs network or a warm Gradle cache.
 *
 * @author TheMeinerLP
 */
class BaseBuildIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void realGradleBuildExportsTheBaseJarAndTheClasspathOfTheFixture() throws IOException {
        Path checkout = tempDir.resolve("clone");
        copyFixture(checkout);
        Path export = tempDir.resolve("export");
        BaseBuildRunner runner = new BaseBuildRunner(new SystemProcessRunner(), Duration.ofMinutes(10),
                System.getenv());

        runner.build(checkout, export);

        assertTrue(Files.isRegularFile(export.resolve("vulpes-base.jar")), "the base jar must be exported");
        assertTrue(Files.isDirectory(export.resolve("lib")), "the classpath directory must be exported");
        try (Stream<Path> entries = Files.list(export)) {
            assertFalse(entries.anyMatch(path -> path.getFileName().toString().equals("classes")),
                    "the export must hold the jar and the classpath only");
        }
    }

    @Test
    void checkoutWithoutWrapperIsRefusedBeforeAnyProcessStarts() throws IOException {
        Path checkout = Files.createDirectories(tempDir.resolve("empty"));
        BaseBuildRunner runner = new BaseBuildRunner(new SystemProcessRunner(), Duration.ofMinutes(1), Map.of());

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(ReleaseRejectedException.Reason.BASE_UNAVAILABLE, e.getReason());
    }

    /** Copies the fixture, the wrapper of this project and the wrapper's jar into the checkout. */
    private void copyFixture(Path checkout) throws IOException {
        Path fixture = Path.of("src/integrationTest/resources/vulpes-base-fixture");
        try (Stream<Path> files = Files.walk(fixture)) {
            for (Path source : files.filter(Files::isRegularFile).toList()) {
                copy(source, checkout.resolve(fixture.relativize(source).toString()));
            }
        }
        Path gradlew = Path.of("gradlew");
        copy(gradlew, checkout.resolve("gradlew"));
        checkout.resolve("gradlew").toFile().setExecutable(true);
        try (Stream<Path> wrapper = Files.walk(Path.of("gradle/wrapper"))) {
            for (Path source : wrapper.filter(Files::isRegularFile).toList()) {
                copy(source, checkout.resolve("gradle/wrapper").resolve(Path.of("gradle/wrapper").relativize(source).toString()));
            }
        }
    }

    private static void copy(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
}
