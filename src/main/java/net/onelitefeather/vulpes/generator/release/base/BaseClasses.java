package net.onelitefeather.vulpes.generator.release.base;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The cached classes of one {@code vulpes-base} commit: the built base jar and the compile and runtime dependency
 * jars that the generated code is compiled against.
 *
 * @param jar       the built {@code vulpes-base} jar
 * @param classpath the jar followed by the dependency jars, sorted by file name
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public record BaseClasses(Path jar, List<Path> classpath) {

    /** The directory of the dependency jars inside a cache entry. */
    static final String LIB_DIR = "lib";

    /**
     * Reads the classes of a cache entry.
     *
     * @param entry the directory of one commit in the cache
     * @return the classes of the entry
     * @throws UncheckedIOException when the entry cannot be read
     */
    public static BaseClasses of(Path entry) {
        Path jar = entry.resolve(BaseBuildRunner.JAR_NAME);
        List<Path> classpath = new ArrayList<>();
        classpath.add(jar);
        Path lib = entry.resolve(LIB_DIR);
        if (Files.isDirectory(lib)) {
            try (Stream<Path> jars = Files.list(lib)) {
                jars.filter(path -> path.getFileName().toString().endsWith(".jar")).sorted().forEach(classpath::add);
            } catch (IOException e) {
                throw new UncheckedIOException("cannot read the dependency jars of " + entry, e);
            }
        }
        return new BaseClasses(jar, List.copyOf(classpath));
    }
}
