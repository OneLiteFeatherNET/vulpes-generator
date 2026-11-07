package net.onelitefeather.vulpes.generator.release.base;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.domain.configuration.BaseConfiguration;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a {@code vulpes-base} checkout with the Gradle wrapper of that checkout. The build runs with a fixed
 * allowlist of environment variables, so that no generator secret reaches code of the branch. The generator's own
 * init script adds the {@value #EXPORT_TASK} task, which copies the built jar and the compile and runtime
 * dependency jars into the export directory. The checkout itself is not changed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class BaseBuildRunner implements BaseBuild {

    /** The name of the task the init script adds to the base build. */
    static final String EXPORT_TASK = "vulpesExportBase";
    /** The file name of the exported base jar. */
    static final String JAR_NAME = "vulpes-base.jar";
    private static final String INIT_SCRIPT_RESOURCE = "/vulpes-base-export.init.gradle";
    private static final String INIT_SCRIPT_NAME = "vulpes-export.init.gradle";
    private static final String LOG_NAME = "vulpes-base-build.log";
    /** Environment variables that the build may inherit; every other variable is dropped. */
    private static final List<String> INHERITED = List.of("PATH", "HOME", "JAVA_HOME", "LANG", "LC_ALL", "TMPDIR",
            "GRADLE_USER_HOME");

    private final ProcessRunner processes;
    private final Duration buildTimeout;
    private final Map<String, String> parentEnvironment;

    /**
     * Creates the runner with the configured build time limit.
     *
     * @param processes the process runner
     * @param base      the {@code vulpes.base.*} configuration with the build time limit
     */
    @Inject
    public BaseBuildRunner(ProcessRunner processes, BaseConfiguration base) {
        this(processes, base.buildTimeout(), System.getenv());
    }

    /**
     * Creates the runner for explicit values. The tests pass a fake process runner and a parent environment.
     *
     * @param processes         the process runner
     * @param buildTimeout      the time limit of one build
     * @param parentEnvironment the environment the allowed variables are copied from
     */
    public BaseBuildRunner(ProcessRunner processes, Duration buildTimeout, Map<String, String> parentEnvironment) {
        this.processes = processes;
        this.buildTimeout = buildTimeout;
        this.parentEnvironment = Map.copyOf(parentEnvironment);
    }

    @Override
    public void build(Path checkout, Path export) {
        Path wrapper = checkout.resolve("gradlew");
        if (!Files.isRegularFile(wrapper)) {
            throw unavailable("vulpes-base has no Gradle wrapper at the resolved commit");
        }
        try {
            Files.createDirectories(export);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create export directory " + export, e);
        }
        Path initScript = writeInitScript(checkout);
        List<String> command = List.of(wrapper.toAbsolutePath().toString(), "--no-daemon", "--console=plain",
                "--no-configuration-cache", "--init-script", initScript.toAbsolutePath().toString(),
                "-PvulpesExportDir=" + export.toAbsolutePath(), EXPORT_TASK);
        ProcessRunner.ProcessResult result = processes.run(command, checkout, environment(), buildTimeout,
                checkout.resolveSibling(LOG_NAME));
        if (result.timedOut()) {
            throw unavailable("the vulpes-base build exceeded the time limit of " + minutes(buildTimeout));
        }
        if (result.exitCode() != 0) {
            throw unavailable("the vulpes-base build failed with exit code " + result.exitCode());
        }
        if (!Files.isRegularFile(export.resolve(JAR_NAME))) {
            throw unavailable("the vulpes-base build exported no jar");
        }
    }

    private Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        for (String name : INHERITED) {
            String value = parentEnvironment.get(name);
            if (value != null) {
                environment.put(name, value);
            }
        }
        return Map.copyOf(environment);
    }

    private Path writeInitScript(Path checkout) {
        Path script = checkout.resolveSibling(INIT_SCRIPT_NAME);
        try (InputStream in = BaseBuildRunner.class.getResourceAsStream(INIT_SCRIPT_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing resource " + INIT_SCRIPT_RESOURCE);
            }
            Files.write(script, in.readAllBytes());
            return script;
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the init script " + script, e);
        }
    }

    private static String minutes(Duration duration) {
        return duration.toSeconds() % 60 == 0 ? duration.toMinutes() + " minutes" : duration.toSeconds() + " seconds";
    }

    private static ReleaseRejectedException unavailable(String message) {
        return new ReleaseRejectedException(Reason.BASE_UNAVAILABLE, message);
    }
}
