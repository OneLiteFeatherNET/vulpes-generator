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
 * Compiles generated source files in process, so a test can prove that the output is valid Java.
 */
public final class JavaSourceAssertions {

    private JavaSourceAssertions() {
    }

    /**
     * Asserts that the generated sources below the given root compile against the test classpath.
     *
     * @param root        the directory the generator wrote into
     * @param sourcePaths the source paths relative to the root, all of them are compiled together
     * @throws IOException if the class output directory cannot be created
     */
    public static void assertCompiles(Path root, String... sourcePaths) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classesDir = Files.createDirectories(root.resolve("classes"));
        String[] arguments = new String[sourcePaths.length + 5];
        arguments[0] = "-proc:none";
        arguments[1] = "-classpath";
        arguments[2] = System.getProperty("java.class.path");
        arguments[3] = "-d";
        arguments[4] = classesDir.toString();
        for (int i = 0; i < sourcePaths.length; i++) {
            arguments[i + 5] = root.resolve(sourcePaths[i]).toString();
        }

        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode;
        try (PrintStream err = new PrintStream(errors, true, StandardCharsets.UTF_8)) {
            exitCode = compiler.run(null, OutputStream.nullOutputStream(), err, arguments);
        }

        assertEquals(0, exitCode, "The generated sources " + String.join(", ", sourcePaths)
                + " must compile but the compiler reported:\n" + errors.toString(StandardCharsets.UTF_8));
    }
}
