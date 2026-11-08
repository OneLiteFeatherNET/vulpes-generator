package net.onelitefeather.vulpes.generator.release.base;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseBuildCredentials;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
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
    private static final ProcessRunner.ProcessResult SUCCESS = new ProcessRunner.ProcessResult(0, false);
    private static final ProcessRunner.ProcessResult FAILURE = new ProcessRunner.ProcessResult(1, false);
    private static final ProcessRunner.ProcessResult TIMEOUT = new ProcessRunner.ProcessResult(-1, true);
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

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(BaseBuildRunner.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

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

    @Test
    void buildThatFailsOnceThenSucceedsRunsTwiceAndProducesTheExport() throws IOException {
        Path checkout = checkoutWithWrapper();
        Path export = tempDir.resolve("export");
        FakeProcessRunner processes = attempts(export, FAILURE, SUCCESS);
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        runner.build(checkout, export);

        assertEquals(2, processes.calls().size(), "a failed build must be retried exactly once");
        assertTrue(Files.isRegularFile(export.resolve(BaseBuildRunner.JAR_NAME)),
                "the retried build must produce the export");
    }

    @Test
    void buildFailingTwiceGivesBaseUnavailableWithTheExitCodeOfTheSecondAttempt() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = attempts(tempDir.resolve("export"),
                new ProcessRunner.ProcessResult(3, false), new ProcessRunner.ProcessResult(7, false));
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
        assertTrue(e.getMessage().contains("failed with exit code 7"),
                "the message must name the exit code of the last attempt: " + e.getMessage());
        assertEquals(2, processes.calls().size(), "a build that fails twice must run exactly twice");
    }

    @Test
    void timedOutBuildIsNotRetried() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = attempts(tempDir.resolve("export"), TIMEOUT);
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(1, processes.calls().size(), "a build that hit the time limit must not be retried");
        assertTrue(e.getMessage().contains("time limit"), "the message must name the time limit: " + e.getMessage());
    }

    @Test
    void secondAttemptTimingOutGivesTheTimeLimitError() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = attempts(tempDir.resolve("export"), FAILURE, TIMEOUT);
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> runner.build(checkout, tempDir.resolve("export")));

        assertEquals(Reason.BASE_UNAVAILABLE, e.getReason());
        assertTrue(e.getMessage().contains("time limit"), "the message must name the time limit: " + e.getMessage());
    }

    @Test
    void failedAttemptLogsAWarningWithTheLastFortyLinesOfTheBuildLog() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = attempts(tempDir.resolve("export"), FAILURE, SUCCESS);
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        runner.build(checkout, tempDir.resolve("export"));

        assertEquals(1, appender.list.size(), "exactly one warning must be logged for the failed attempt");
        ILoggingEvent event = appender.list.getFirst();
        assertEquals(Level.WARN, event.getLevel(), "a failed attempt must be logged as a WARN");
        String message = event.getFormattedMessage();
        assertTrue(message.contains("step-50"), "the warning must contain the last line of the log: " + message);
        assertTrue(message.contains("step-11"), "the warning must contain the fortieth line from the end: " + message);
        assertFalse(message.contains("step-10"), "the warning must not contain lines older than forty: " + message);
    }

    @Test
    void warningForFailedAttemptSaysWhenTheBuildLogCannotBeRead() throws IOException {
        Path checkout = checkoutWithWrapper();
        FakeProcessRunner processes = new FakeProcessRunner(call -> FAILURE);
        BaseBuildRunner runner = new BaseBuildRunner(processes, BUILD_TIMEOUT, PARENT_ENVIRONMENT);

        assertThrows(ReleaseRejectedException.class, () -> runner.build(checkout, tempDir.resolve("export")));

        assertTrue(appender.list.getFirst().getFormattedMessage().contains("cannot be read"),
                "a missing build log must be named in the warning: " + appender.list.getFirst().getFormattedMessage());
    }

    private Path checkoutWithWrapper() throws IOException {
        Path checkout = Files.createDirectories(tempDir.resolve("clone"));
        Files.writeString(checkout.resolve("gradlew"), "#!/bin/sh\n", StandardCharsets.UTF_8);
        return checkout;
    }

    /** A runner that behaves like a successful Gradle build: it writes the jar and one dependency into the export. */
    private static FakeProcessRunner exportingRunner(Path export) {
        return new FakeProcessRunner(call -> {
            writeExport(export);
            return SUCCESS;
        });
    }

    /**
     * A runner that answers the attempts in the given order. Every attempt writes fifty numbered lines into its log,
     * as Gradle would, and a successful attempt exports the jar. An attempt beyond the given results fails the test.
     */
    private static FakeProcessRunner attempts(Path export, ProcessRunner.ProcessResult... results) {
        Iterator<ProcessRunner.ProcessResult> answers = List.of(results).iterator();
        return new FakeProcessRunner(call -> {
            writeLog(call.log());
            ProcessRunner.ProcessResult result = answers.next();
            if (result.exitCode() == 0) {
                writeExport(export);
            }
            return result;
        });
    }

    private static void writeExport(Path export) {
        try {
            Files.createDirectories(export.resolve("lib"));
            Files.writeString(export.resolve(BaseBuildRunner.JAR_NAME), "jar", StandardCharsets.UTF_8);
            Files.writeString(export.resolve("lib").resolve("minestom.jar"), "jar", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeLog(Path log) {
        StringBuilder lines = new StringBuilder();
        for (int line = 1; line <= 50; line++) {
            lines.append(String.format("step-%02d%n", line));
        }
        try {
            Files.writeString(log, lines.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
