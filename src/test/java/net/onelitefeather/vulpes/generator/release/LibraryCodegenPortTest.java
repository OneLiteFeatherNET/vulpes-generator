package net.onelitefeather.vulpes.generator.release;

import net.onelitefeather.vulpes.generator.release.codegen.CodegenRefusedException;
import net.onelitefeather.vulpes.generator.release.codegen.GeneratedFile;
import net.onelitefeather.vulpes.generator.release.codegen.LibraryCodegenPort;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the adapter to the published code generator with the sample project of the release tests. The generator runs
 * in memory; no file is written and no base is needed.
 *
 * @author TheMeinerLP
 */
class LibraryCodegenPortTest {

    private static final List<String> FOREIGN = List.of("minecraft", "olf");
    private static final String BASE_COMMIT = "3018dec4de6b2a4dedded228b5e716a17f64a507";

    private final LibraryCodegenPort port = new LibraryCodegenPort();

    @Test
    void generatesTheSourcesOfTheSnapshotInThePackageOfTheProject() {
        byte[] snapshot = snapshot();
        byte[] releaseJson = releaseJson(snapshot);

        List<GeneratedFile> files = port.generate(releaseJson, snapshot, "net.example.content", "eldoria");

        assertFalse(files.isEmpty(), "the sample project must produce sources");
        assertTrue(files.stream().allMatch(file -> file.relativePath().startsWith("net/example/content/")),
                "every source must lie in the package of the project: " + files);
        assertTrue(files.stream().allMatch(file -> file.relativePath().endsWith(".java")),
                "every generated file must be a Java source: " + files);
    }

    @Test
    void sourcesCarryTheDeclaredPackage() {
        byte[] snapshot = snapshot();

        List<GeneratedFile> files = port.generate(releaseJson(snapshot), snapshot, "net.example.content", "eldoria");

        assertTrue(files.stream().anyMatch(file -> file.content().startsWith("package net.example.content;")),
                "a source must declare the requested package");
    }

    @Test
    void releaseJsonThatIsNotFormatOneOrTwoIsRefusedAsGenerationRefused() {
        byte[] snapshot = snapshot();

        CodegenRefusedException e = assertThrows(CodegenRefusedException.class,
                () -> port.generate("{\"format\":9}".getBytes(StandardCharsets.UTF_8), snapshot,
                        "net.example.content", "eldoria"));

        assertTrue(e.getMessage().contains("release.json"), "the message must name the refused file: " + e.getMessage());
    }

    @Test
    void snapshotThatDoesNotMatchTheRevisionIsRefusedAsGenerationRefused() {
        byte[] snapshot = snapshot();
        byte[] other = "{\"format\":1}".getBytes(StandardCharsets.UTF_8);

        assertThrows(CodegenRefusedException.class,
                () -> port.generate(releaseJson(snapshot), other, "net.example.content", "eldoria"));
    }

    private static byte[] snapshot() {
        return new ProjectSnapshotMapper(FOREIGN).map(SampleProject.load().build()).bytes();
    }

    private static byte[] releaseJson(byte[] snapshot) {
        ReleaseRequest request = new ReleaseRequest(SampleProject.ID, "eldoria", "1.0.0",
                SnapshotVerifier.digest(snapshot), "test", Instant.parse("2026-10-28T12:00:00Z"),
                "OneLiteFeatherNET/vulpes-base", "develop", BASE_COMMIT);
        return new ReleaseJsonWriter().write(request, "test");
    }
}
