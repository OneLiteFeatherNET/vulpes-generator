package net.onelitefeather.vulpes.generator.release.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A code generator that generates what the test chooses, and records the arguments of every call.
 *
 * @author TheMeinerLP
 */
final class FakeCodegenPort implements CodegenPort {

    /** The arguments of one call. */
    record Call(byte[] releaseJson, byte[] snapshotJson, String packageName, String projectName) {
    }

    private final List<Call> calls = new ArrayList<>();
    private final Function<Call, List<GeneratedFile>> behaviour;

    FakeCodegenPort(Function<Call, List<GeneratedFile>> behaviour) {
        this.behaviour = behaviour;
    }

    @Override
    public List<GeneratedFile> generate(byte[] releaseJson, byte[] snapshotJson, String packageName,
                                        String projectName) {
        Call call = new Call(releaseJson, snapshotJson, packageName, projectName);
        calls.add(call);
        return behaviour.apply(call);
    }

    List<Call> calls() {
        return List.copyOf(calls);
    }
}
