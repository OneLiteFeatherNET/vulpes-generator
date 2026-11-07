package net.onelitefeather.vulpes.generator.release;

import net.onelitefeather.vulpes.generator.release.codegen.GeneratedFile;
import net.onelitefeather.vulpes.generator.release.codegen.LibraryCodegenPort;
import net.onelitefeather.vulpes.generator.release.compile.CompilationGate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the published codegen library on the sample project and compiles the generated sources in-process against
 * {@code vulpes-base} 0.6.0, the base the minestom template targets. Needs the Vulpes repository for the libraries.
 *
 * @author TheMeinerLP
 */
class CodegenIntegrationTest {

    private static final List<String> FOREIGN = List.of("minecraft", "olf");
    private static final String BASE_COMMIT = "3018dec4de6b2a4dedded228b5e716a17f64a507";

    @TempDir
    Path tempDir;

    @Test
    void generatedSourcesOfTheSampleProjectCompileAgainstVulpesBase() throws IOException {
        byte[] snapshot = new ProjectSnapshotMapper(FOREIGN).map(SampleProject.load().build()).bytes();
        ReleaseRequest request = new ReleaseRequest(SampleProject.ID, "eldoria", "1.0.0",
                SnapshotVerifier.digest(snapshot), "test", Instant.parse("2026-10-28T12:00:00Z"),
                "OneLiteFeatherNET/vulpes-base", "develop", BASE_COMMIT);
        byte[] releaseJson = new ReleaseJsonWriter().write(request, "test");
        List<GeneratedFile> files = new LibraryCodegenPort()
                .generate(releaseJson, snapshot, "net.example.content", "eldoria");
        Path generated = tempDir.resolve("generated");
        for (GeneratedFile file : files) {
            Path target = generated.resolve(file.relativePath());
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.content(), StandardCharsets.UTF_8);
        }

        Path classes = new CompilationGate().compile(generated, tempDir.resolve("classes"), classpath());

        try (Stream<Path> written = Files.walk(classes)) {
            assertTrue(written.anyMatch(path -> path.toString().endsWith(".class")),
                    "the generated sources must compile to class files");
        }
    }

    @Test
    void generatedSourcesAreNotEmpty() {
        byte[] snapshot = new ProjectSnapshotMapper(FOREIGN).map(SampleProject.load().build()).bytes();
        ReleaseRequest request = new ReleaseRequest(SampleProject.ID, "eldoria", "1.0.0",
                SnapshotVerifier.digest(snapshot), "test", Instant.parse("2026-10-28T12:00:00Z"),
                "OneLiteFeatherNET/vulpes-base", "develop", BASE_COMMIT);

        List<GeneratedFile> files = new LibraryCodegenPort()
                .generate(new ReleaseJsonWriter().write(request, "test"), snapshot, "net.example.content", "eldoria");

        assertFalse(files.isEmpty(), "the sample project must produce sources for the minestom template");
    }

    /** The compile classpath of this test run: the base jar and its dependencies, as Gradle put them on the JVM. */
    private static List<Path> classpath() {
        return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).map(Path::of).toList();
    }
}
