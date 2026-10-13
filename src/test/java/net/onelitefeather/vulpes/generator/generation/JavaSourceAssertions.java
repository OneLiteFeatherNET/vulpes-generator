package net.onelitefeather.vulpes.generator.generation;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Compiles a generated source file in process, so a test can prove that the output is valid Java.
 */
public final class JavaSourceAssertions {

    private JavaSourceAssertions() {
    }

    /**
     * Asserts that the generated source below the given root compiles against the test classpath.
     *
     * @param root       the directory the generator wrote into
     * @param sourcePath the source path relative to the root
     * @throws IOException if the class output directory cannot be created
     */
    public static void assertCompiles(Path root, String sourcePath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classesDir = Files.createDirectories(root.resolve("classes"));
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            exitCode = compiler.run(null, OutputStream.nullOutputStream(), err,
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classesDir.toString(),
                    root.resolve(sourcePath).toString());
        }

        assertEquals(0, exitCode, "The generated " + sourcePath + " must compile but the compiler reported:\n"
                + errors.toString(StandardCharsets.UTF_8));
    }
}
