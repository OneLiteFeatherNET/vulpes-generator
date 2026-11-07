package net.onelitefeather.vulpes.generator.release.compile;

import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the compilation gate against a fixture base in a temporary directory, not against the real vulpes-base.
 *
 * @author TheMeinerLP
 */
class CompilationGateTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedSourcesThatMatchTheBaseCompileWithoutDiagnostics() throws IOException {
        Path base = compiledFixtureBase();
        Path generated = source("items/Sword.java", """
                package items;

                public final class Sword {
                    public String describe() {
                        return net.onelitefeather.vulpes.fixture.Base.name();
                    }
                }
                """);
        Path classes = tempDir.resolve("request").resolve("classes");

        Path output = new CompilationGate().compile(generated, classes, List.of(base));

        assertEquals(classes, output);
        assertTrue(Files.isRegularFile(classes.resolve("items").resolve("Sword.class")),
                "the compiled class must be written into the request directory");
    }

    @Test
    void compilationWritesNothingIntoTheSourceRoot() throws IOException {
        Path base = compiledFixtureBase();
        Path generated = source("items/Sword.java", """
                package items;

                public final class Sword {
                }
                """);

        new CompilationGate().compile(generated, tempDir.resolve("classes"), List.of(base));

        try (var files = Files.walk(generated)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".class")),
                    "the generated root must hold sources only");
        }
    }

    @Test
    void typeErrorGivesCompilationFailedWithFileLineAndMessage() throws IOException {
        Path base = compiledFixtureBase();
        Path generated = source("items/Sword.java", """
                package items;

                public final class Sword {
                    int damage = "twelve";
                }
                """);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> new CompilationGate().compile(generated, tempDir.resolve("classes"), List.of(base)));

        assertEquals(Reason.COMPILATION_FAILED, e.getReason());
        assertEquals(1, e.getDiagnostics().size(), "one error must give one diagnostic: " + e.getDiagnostics());
        ReleaseDiagnostic diagnostic = e.getDiagnostics().getFirst();
        assertEquals("items/Sword.java", diagnostic.file(), "the file is relative to the generated root");
        assertEquals(4, diagnostic.line(), "the line of the error must be reported");
        assertTrue(diagnostic.message().contains("String"), "the compiler message must be passed on: "
                + diagnostic.message());
        assertFalse(e.isTruncated());
    }

    @Test
    void moreThanOneHundredErrorsGiveExactlyHundredDiagnosticsAndTruncated() throws IOException {
        Path base = compiledFixtureBase();
        String body = IntStream.range(0, 150)
                .mapToObj(i -> "    int value" + i + " = \"text\";")
                .collect(Collectors.joining("\n"));
        Path generated = source("items/Broken.java", "package items;\n\npublic final class Broken {\n"
                + body + "\n}\n");

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> new CompilationGate().compile(generated, tempDir.resolve("classes"), List.of(base)));

        assertEquals(100, e.getDiagnostics().size(), "the list must be capped at 100 diagnostics");
        assertTrue(e.isTruncated(), "the response must state that the list was cut");
    }

    @Test
    void errorsUpToTheCapAreNotMarkedTruncated() throws IOException {
        Path base = compiledFixtureBase();
        Path generated = source("items/Few.java", """
                package items;

                public final class Few {
                    int a = "x";
                    int b = "y";
                }
                """);

        ReleaseRejectedException e = assertThrows(ReleaseRejectedException.class,
                () -> new CompilationGate().compile(generated, tempDir.resolve("classes"), List.of(base)));

        assertEquals(2, e.getDiagnostics().size());
        assertFalse(e.isTruncated(), "a list that is not cut must not be marked as truncated");
    }

    private Path source(String relative, String content) throws IOException {
        Path root = tempDir.resolve("generated");
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return root;
    }

    /** Compiles a small fixture base into a classes directory, the stand-in for the cached vulpes-base classes. */
    private Path compiledFixtureBase() throws IOException {
        Path source = tempDir.resolve("fixture-src").resolve("net/onelitefeather/vulpes/fixture/Base.java");
        Files.createDirectories(source.getParent());
        Files.writeString(source, """
                package net.onelitefeather.vulpes.fixture;

                public final class Base {
                    public static String name() {
                        return "base";
                    }
                }
                """, StandardCharsets.UTF_8);
        Path classes = tempDir.resolve("fixture-classes");
        Files.createDirectories(classes);
        int status = ToolProvider.getSystemJavaCompiler().run(null, null, null,
                "--release", "25", "-d", classes.toString(), source.toString());
        assertEquals(0, status, "the fixture base must compile");
        return classes;
    }
}
