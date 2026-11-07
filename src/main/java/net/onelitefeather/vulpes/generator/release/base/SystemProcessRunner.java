package net.onelitefeather.vulpes.generator.release.base;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs processes with {@link ProcessBuilder}. The inherited environment is cleared, so the process gets exactly the
 * environment it is handed.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class SystemProcessRunner implements ProcessRunner {

    @Override
    public ProcessResult run(List<String> command, Path directory, Map<String, String> environment,
                             Duration timeout, Path log) {
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(log.toFile());
        builder.environment().clear();
        builder.environment().putAll(environment);
        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                    "cannot start the base build: " + e.getMessage());
        }
        try {
            if (process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                return new ProcessResult(process.exitValue(), false);
            }
            process.destroyForcibly();
            process.waitFor();
            return new ProcessResult(-1, true);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new ReleaseRejectedException(ReleaseRejectedException.Reason.BASE_UNAVAILABLE,
                    "the base build was interrupted");
        }
    }
}
