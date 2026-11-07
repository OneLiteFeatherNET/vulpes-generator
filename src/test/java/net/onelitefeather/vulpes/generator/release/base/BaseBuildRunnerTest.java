package net.onelitefeather.vulpes.generator.release.base;

import net.onelitefeather.vulpes.generator.domain.configuration.BaseBuildCredentials;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Gradle invocation of a base build with a fake process runner. No Gradle process starts.
 *
 * @author TheMeinerLP
 */
class BaseBuildRunnerTest {

    private static final Duration BUILD_TIMEOUT = Duration.ofMinutes(10);
    private static final Map<String, String> PARENT_ENVIRONMENT = Map.of(
            "PATH", "/usr/bin",
            "HOME", "/home/generator",
            "JAVA_HOME", "/opt/java",
            "GRADLE_USER_HOME", "/cache/gradle",
            "ONELITEFEATHER_MAVEN_USERNAME", "maven-user",
            "ONELITEFEATHER_MAVEN_PASSWORD", "maven-secret",
            "GITHUB_PASSWORD", "github-secret",
            "VULPES_BACKEND_TOKEN", "backend-secret",
            "VULPES_RELEASE_TOKEN", "release-secret",
            "DB_PASSWORD", "database-secret");

    @TempDir
    Path tempDir;

    @Test
    void runsTheClonesOwnGradleWrapperInsideTheCheckout() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));

        new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT).build(checkout, tempDir.resolve("export"));

        FakeProcessRunner.Call call = processes.calls().getFirst();
        assertEquals(checkout.resolve("gradlew").toAbsolutePath().toString(), call.command().getFirst(),
                "the build must run the wrapper of the checkout");
        assertEquals(checkout, call.directory(), "the wrapper must run inside the checkout");
    }

    @Test
    void invokesTheExportTaskWithTheExportDirectoryAndTheOwnInitScript() throws IOException {
        Path checkout = checkoutWithWrapper();
        Path export = tempDir.resolve("export");
        FakeProcessRunner processes = exportingRunner(export);

        new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT).build(checkout, export);

        List<String> command = processes.calls().getFirst().command();
        assertEquals("vulpesExportBase", command.getLast(), "the export task must be the requested task");
        assertTrue(command.contains("-PvulpesExportDir=" + export.toAbsolutePath()),
                "the export directory must be passed as a project property: " + command);
        int initScript = command.indexOf("--init-script");
        assertTrue(initScript >= 0, "the generator's init script must be passed: " + command);
        Path script = Path.of(command.get(initScript + 1));
        assertTrue(Files.readString(script, StandardCharsets.UTF_8).contains("vulpesExportBase"),
                "the init script must register the export task");
        assertFalse(script.startsWith(checkout), "the init script must not be written into the checkout");
    }

    @Test
    void environmentOfTheBuildHoldsNoGeneratorSecret() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));

        new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT).build(checkout, tempDir.resolve("export"));

        Map<String, String> environment = processes.calls().getFirst().environment();
        assertFalse(environment.containsKey("ONELITEFEATHER_MAVEN_USERNAME"));
        assertFalse(environment.containsKey("ONELITEFEATHER_MAVEN_PASSWORD"));
        assertFalse(environment.containsKey("GITHUB_PASSWORD"));
        assertFalse(environment.containsKey("DB_PASSWORD"), "variables outside the allowlist must not be passed");
        assertTrue(environment.keySet().stream().noneMatch(name -> name.startsWith("VULPES_")),
                "no VULPES_ variable may reach the build: " + environment.keySet());
        assertEquals("/usr/bin", environment.get("PATH"), "the build needs PATH to start the wrapper");
        assertEquals("/cache/gradle", environment.get("GRADLE_USER_HOME"),
                "the build keeps the shared Gradle cache");
    }

    @Test
    void passesTheBuildTimeoutToTheProcessRunner() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));

        new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT).build(checkout, tempDir.resolve("export"));

        assertEquals(BUILD_TIMEOUT, processes.calls().getFirst().timeout(),
                "the injected build time limit must be the limit of the process");
    }

    @Test
    void buildExceedingTheTimeLimitGivesBaseUnavailableNamingTheLimit() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = new FakeProcessRunner(call -> new ProcessRunner.ProcessResult(-1, true));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
        assertTrue(e.getMessage().contains("time limit"), "the message must name the time limit: " + e.getMessage());
    }

    @Test
    void failingGradleBuildGivesBaseUnavailable() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = new FakeProcessRunner(call -> new ProcessRunner.ProcessResult(1, false));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
    }

    @Test
    void buildWithoutExportedJarGivesBaseUnavailable() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = new FakeProcessRunner(call -> new ProcessRunner.ProcessResult(0, false));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason(), "a build that exports no jar is not a usable base");
    }

    @Test
    void checkoutWithoutGradleWrapperGivesBaseUnavailableWithoutStartingAProcess() throws IOException {
        Path checkout = Files.createDirectories(tempDir.resolve("clone"));
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
        assertTrue(processes.calls().isEmpty(), "no process may start without a wrapper");
    }

    @Test
    void passesTheReadOnlyCredentialUnderTheGradleProjectNames() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT,
                new BaseBuildCredentials("reader", "reader-secret"));

        runner.build(checkout, tempDir.resolve("export"));

        Map<String, String> environment = processes.calls().getFirst().environment();
        assertEquals("reader", environment.get("ORG_GRADLE_PROJECT_OneLiteFeatherRepositoryUsername"),
                "the read-only user must reach the build under the Gradle project name");
        assertEquals("reader-secret", environment.get("ORG_GRADLE_PROJECT_OneLiteFeatherRepositoryPassword"),
                "the read-only password must reach the build under the Gradle project name");
    }

    @Test
    void neverPassesThePublishCredentialToTheBuild() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT,
                new BaseBuildCredentials("reader", "reader-secret"));

        runner.build(checkout, tempDir.resolve("export"));

        Map<String, String> environment = processes.calls().getFirst().environment();
        assertFalse(environment.containsValue("maven-user"), "the publish user must never reach the build");
        assertFalse(environment.containsValue("maven-secret"), "the publish password must never reach the build");
    }

    @Test
    void passesNoRepositoryVariableWithoutTheReadOnlyCredential() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = exportingRunner(tempDir.resolve("export"));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT,
                new BaseBuildCredentials("", ""));

        runner.build(checkout, tempDir.resolve("export"));

        Map<String, String> environment = processes.calls().getFirst().environment();
        assertTrue(environment.keySet().stream().noneMatch(name -> name.startsWith("ORG_GRADLE_PROJECT_OneLiteFeatherRepository")),
                "no repository variable may be set without the read-only credential: " + environment.keySet());
    }

    private Path checkoutWithWrapper() throws IOException {
        Path checkout = Files.createDirectories(tempDir.resolve("clone"));
        Files.writeString(checkout.resolve("gradlew"), "#!/bin/sh\n", StandardCharsets.UTF_8);
        return checkout;
    }

    /** A runner that behaves like a successful Gradle build: it writes the jar and one dependency into the export. */
    private static FakeProcessRunner exportingRunner(Path export) {
        return new FakeProcessRunner(call -> {
            try {
                Files.createDirectories(export.resolve("lib"));
                Files.writeString(export.resolve("vulpes-base.jar"), "jar", StandardCharsets.UTF_8);
                Files.writeString(export.resolve("lib").resolve("minestom.jar"), "jar", StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
            return new ProcessRunner.ProcessResult(0, false);
        });
    }
}
