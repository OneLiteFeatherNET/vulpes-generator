package net.onelitefeather.vulpes.generator.release.codegen;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException;
import net.onelitefeather.vulpes.generator.release.ReleaseRejectedException.Reason;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes the sources of the code generator into a request directory. A refusal of the generator becomes
 * {@code GENERATION_REFUSED}, and a file that would leave the request directory is refused the same way.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class CodegenGate {

    private final CodegenPort codegen;

    /**
     * Creates the gate.
     *
     * @param codegen the code generator port
     */
    @Inject
    public CodegenGate(CodegenPort codegen) {
        this.codegen = codegen;
    }

    /**
     * Generates the sources and writes them below the target directory.
     *
     * @param targetDir    the directory that receives the sources; created if missing
     * @param releaseJson  the {@code release.json} bytes handed to the generator
     * @param snapshotJson the snapshot bytes handed to the generator
     * @param packageName  the Java package of the sources
     * @param projectName  the project name of the snapshot
     * @throws ReleaseRejectedException with {@code GENERATION_REFUSED} when the generator refuses the snapshot
     */
    public void generateInto(Path targetDir, byte[] releaseJson, byte[] snapshotJson, String packageName,
                             String projectName) {
        List<GeneratedFile> files;
        try {
            files = codegen.generate(releaseJson, snapshotJson, packageName, projectName);
        } catch (CodegenRefusedException e) {
            throw new ReleaseRejectedException(Reason.GENERATION_REFUSED,
                    "the code generator refused the snapshot: " + e.getMessage());
        }
        Path root = targetDir.toAbsolutePath().normalize();
        for (GeneratedFile file : files) {
            Path target = root.resolve(file.relativePath()).normalize();
            if (file.relativePath().isBlank() || Path.of(file.relativePath()).isAbsolute()
                    || !target.startsWith(root) || target.equals(root)) {
                throw new ReleaseRejectedException(Reason.GENERATION_REFUSED,
                        "the code generator produced a file outside the generated root: " + file.relativePath());
            }
            write(target, file.content());
        }
    }

    private static void write(Path target, String content) {
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot write the generated file " + target, e);
        }
    }
}
