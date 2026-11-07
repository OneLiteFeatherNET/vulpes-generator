package net.onelitefeather.vulpes.generator.release.base;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A process runner that starts no process. It records every command with its directory, environment and time limit,
 * and answers with the result the test chooses.
 *
 * @author TheMeinerLP
 */
final class FakeProcessRunner implements ProcessRunner {

    /**
     * One recorded invocation.
     *
     * @param command     the command line
     * @param directory   the working directory
     * @param environment the environment the process would get
     * @param timeout     the time limit handed to the runner
     * @param log         the file the output would be written to
     */
    record Call(List<String> command, Path directory, Map<String, String> environment, Duration timeout, Path log) {
    }

    private final List<Call> calls = new ArrayList<>();
    private final Function<Call, ProcessResult> behaviour;

    /**
     * Creates the fake.
     *
     * @param behaviour the answer for each call; it may also create files, as Gradle would
     */
    FakeProcessRunner(Function<Call, ProcessResult> behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public ProcessResult run(List<String> command, Path directory, Map<String, String> environment,
                             Duration timeout, Path log) {
        Call call = new Call(List.copyOf(command), directory, Map.copyOf(environment), timeout, log);
        calls.add(call);
        return behaviour.apply(call);
    }

    /**
     * Returns the calls made so far, in order.
     *
     * @return the recorded calls
     */
    List<Call> calls() {
        return List.copyOf(calls);
    }
}
