package net.onelitefeather.vulpes.generator.release.base;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Port for running an external process with a time limit. The base build is the only user; tests replace it with
 * a fake, so that no Gradle process starts in the unit tests.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ProcessRunner {

    /**
     * Runs the command and waits for it, at most for the time limit.
     *
     * @param command     the command line, the first element is the executable
     * @param directory   the working directory of the process
     * @param environment the complete environment of the process; nothing else is inherited
     * @param timeout     the time limit; a process that runs longer is stopped
     * @param log         the file that receives the standard output and the standard error
     * @return the exit code, and whether the time limit stopped the process
     */
    ProcessResult run(List<String> command, Path directory, Map<String, String> environment, Duration timeout,
                      Path log);

    /**
     * The outcome of a process.
     *
     * @param exitCode the exit code, {@code -1} when the time limit stopped the process
     * @param timedOut {@code true} when the time limit stopped the process
     */
    record ProcessResult(int exitCode, boolean timedOut) {
    }
}
