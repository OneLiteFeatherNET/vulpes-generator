package net.onelitefeather.vulpes.generator.release.base;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * A base build that writes a jar and one dependency jar, as a successful build would, and records every checkout it
 * was asked to build. The test may add behaviour before the export.
 *
 * @author TheMeinerLP
 */
final class FakeBaseBuild implements BaseBuild {

    /** Extra behaviour of one build, run before the export is written. */
    @FunctionalInterface
    interface Action {
        void run(Path checkout) throws Exception;
    }

    private final List<Path> checkouts = new ArrayList<>();
    private final Action action;

    FakeBaseBuild() {
        this(checkout -> {
        });
    }

    FakeBaseBuild(Action action) {
        this.action = action;
    }

    @Override
    public void build(Path checkout, Path export) {
        checkouts.add(checkout);
        try {
            action.run(checkout);
            Files.createDirectories(export.resolve("lib"));
            writeJar(export.resolve("vulpes-base.jar"));
            writeJar(export.resolve("lib").resolve("minestom.jar"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Writes a valid jar with one entry, so that the compiler accepts it on the classpath. */
    static void writeJar(Path jar) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            out.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

    /**
     * Returns the number of builds so far.
     *
     * @return the build count
     */
    int builds() {
        return checkouts.size();
    }
}
