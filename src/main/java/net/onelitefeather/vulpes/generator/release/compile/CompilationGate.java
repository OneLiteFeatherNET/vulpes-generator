package net.onelitefeather.vulpes.generator.release.compile;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The gate between generated sources and a release: the sources are compiled in-process with {@code javax.tools},
 * against the cached classes of one base commit, for release 25. The output is written into the request directory
 * and is never published. Compiler errors become diagnostics with file, line and message; the list is capped.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class CompilationGate {

    /** The most diagnostics a refusal carries. */
    static final int MAX_DIAGNOSTICS = 100;
    private static final String RELEASE = "25";
    /** Above javac's default of 100 errors, so that the cap of {@link #MAX_DIAGNOSTICS} can be detected. */
    private static final String MAX_ERRORS = "10000";

    /**
     * Compiles every {@code .java} file below the source root.
     *
     * @param sourceRoot the directory of the generated sources; it is only read
     * @param outputDir  the directory that receives the class files
     * @param classpath  the cached base jar and dependency jars
     * @return the output directory
     * @throws ReleaseRejectedException with {@code COMPILATION_FAILED} and the diagnostics when a file does not
     *                                  compile, and with {@code BASE_UNAVAILABLE} when no compiler is available
     */
    public Path compile(Path sourceRoot, Path outputDir, List<Path> classpath) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ReleaseRejectedException(Reason.BASE_UNAVAILABLE,
                    "no Java compiler is available in the generator runtime");
        }
        List<Path> sources = javaSources(sourceRoot);
        createDirectories(outputDir);
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        boolean success;
        try (StandardJavaFileManager files = compiler.getStandardFileManager(collector, Locale.ROOT,
                StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = files.getJavaFileObjectsFromPaths(sources);
            success = Boolean.TRUE.equals(compiler.getTask(null, files, collector,
                    options(classpath, outputDir), null, units).call());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot compile the generated sources", e);
        }
        List<Diagnostic<? extends JavaFileObject>> errors = collector.getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.ERROR)
                .toList();
        if (success && errors.isEmpty()) {
            return outputDir;
        }
        List<ReleaseDiagnostic> diagnostics = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> error : errors.stream().limit(MAX_DIAGNOSTICS).toList()) {
            diagnostics.add(toDiagnostic(sourceRoot, error));
        }
        boolean truncated = errors.size() > MAX_DIAGNOSTICS;
        String message = errors.isEmpty()
                ? "the generated sources do not compile against the base"
                : "the generated sources do not compile against the base: " + errors.size() + " error"
                + (errors.size() == 1 ? "" : "s");
        throw new ReleaseRejectedException(Reason.COMPILATION_FAILED, message, diagnostics, truncated);
    }

    private static List<String> options(List<Path> classpath, Path outputDir) {
        List<String> options = new ArrayList<>(List.of("--release", RELEASE, "-encoding", "UTF-8", "-proc:none",
                "-Xmaxerrs", MAX_ERRORS));
        if (!classpath.isEmpty()) {
            options.add("-classpath");
            options.add(classpath.stream().map(Path::toString).reduce((a, b) -> a + File.pathSeparator + b)
                    .orElse(""));
        }
        options.add("-d");
        options.add(outputDir.toAbsolutePath().toString());
        return options;
    }

    private static ReleaseDiagnostic toDiagnostic(Path root, Diagnostic<? extends JavaFileObject> error) {
        long line = error.getLineNumber() == Diagnostic.NOPOS ? 0 : error.getLineNumber();
        return new ReleaseDiagnostic(relativeName(root, error.getSource()), line, error.getMessage(Locale.ROOT));
    }

    private static String relativeName(Path root, JavaFileObject source) {
        if (source == null) {
            return "";
        }
        Path file = Path.of(source.toUri()).toAbsolutePath().normalize();
        Path base = root.toAbsolutePath().normalize();
        return file.startsWith(base) ? base.relativize(file).toString().replace('\\', '/')
                : file.getFileName().toString();
    }

    private static List<Path> javaSources(Path root) {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read the generated sources in " + root, e);
        }
    }

    private static void createDirectories(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot create " + dir, e);
        }
    }
}
