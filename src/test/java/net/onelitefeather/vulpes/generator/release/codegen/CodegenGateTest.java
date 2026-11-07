package net.onelitefeather.vulpes.generator.release.codegen;

import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the wrapper around the code generator with a fake port. No generator library is loaded.
 *
 * @author TheMeinerLP
 */
class CodegenGateTest {

    private static final byte[] RELEASE = "{\"format\":2}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SNAPSHOT = "{\"format\":1}".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    @Test
    void refusalOfTheGeneratorBecomesGenerationRefusedWithItsMessage() {
        FakeCodegenPort port = new FakeCodegenPort(call -> {
            throw new CodegenRefusedException("advancement 'quest/start' has no Java form");
        });
        CodegenGate gate = new CodegenGate(port);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> gate.generateInto(tempDir.resolve("generated"), RELEASE, SNAPSHOT, "example.pack", "example"));

        assertEquals(Reason.GENERATION_REFUSED, e.getReason());
        assertTrue(e.getMessage().contains("advancement 'quest/start' has no Java form"),
                "the message must name the refused content: " + e.getMessage());
    }

    @Test
    void generatorReceivesReleaseJsonSnapshotPackageAndProjectName() {
        FakeCodegenPort port = new FakeCodegenPort(call -> List.of());
        CodegenGate gate = new CodegenGate(port);

        gate.generateInto(tempDir.resolve("generated"), RELEASE, SNAPSHOT, "example.pack", "example");

        FakeCodegenPort.Call call = port.calls().getFirst();
        assertEquals("{\"format\":2}", new String(call.releaseJson(), StandardCharsets.UTF_8));
        assertEquals("{\"format\":1}", new String(call.snapshotJson(), StandardCharsets.UTF_8));
        assertEquals("example.pack", call.packageName());
        assertEquals("example", call.projectName());
    }

    @Test
    void generatedFilesAreWrittenBelowTheTargetDirectory() throws IOException {
        Path target = tempDir.resolve("generated");
        FakeCodegenPort port = new FakeCodegenPort(call -> List.of(
                new GeneratedFile("items/Sword.java", "package items;\n")));

        new CodegenGate(port).generateInto(target, RELEASE, SNAPSHOT, "example.pack", "example");

        assertEquals("package items;\n", Files.readString(target.resolve("items").resolve("Sword.java")));
    }

    @Test
    void fileThatLeavesTheTargetDirectoryIsRefusedAndNothingIsWritten() {
        Path target = tempDir.resolve("generated");
        FakeCodegenPort port = new FakeCodegenPort(call -> List.of(
                new GeneratedFile("../escape.java", "class Escape {}")));
        CodegenGate gate = new CodegenGate(port);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> gate.generateInto(target, RELEASE, SNAPSHOT, "example.pack", "example"));

        assertEquals(Reason.GENERATION_REFUSED, e.getReason());
        assertFalse(Files.exists(tempDir.resolve("escape.java")), "no file may be written outside the target");
    }

    @Test
    void absolutePathIsRefused() {
        FakeCodegenPort port = new FakeCodegenPort(call -> List.of(
                new GeneratedFile(tempDir.resolve("absolute.java").toString(), "class Absolute {}")));
        CodegenGate gate = new CodegenGate(port);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> gate.generateInto(tempDir.resolve("generated"), RELEASE, SNAPSHOT, "example.pack", "example"));

        assertEquals(Reason.GENERATION_REFUSED, e.getReason());
        assertFalse(Files.exists(tempDir.resolve("absolute.java")));
    }
}
