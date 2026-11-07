package net.onelitefeather.vulpes.generator.release.codegen;

import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.codegen.CodegenException;
import net.onelitefeather.vulpes.codegen.CodegenOptions;
import net.onelitefeather.vulpes.codegen.CodegenTemplate;
import net.onelitefeather.vulpes.codegen.GeneratedSource;
import net.onelitefeather.vulpes.codegen.VulpesCodegen;
import net.onelitefeather.vulpes.format.ContentFormatException;
import net.onelitefeather.vulpes.format.ContentReader;
import net.onelitefeather.vulpes.format.ContentRelease;

import java.util.List;

/**
 * The production {@link CodegenPort}: parses the release with {@code vulpes-format} and generates the Java sources
 * of the {@code minestom} template with the published {@code vulpes-codegen} library. The generator's own code is
 * never copied into this repository.
 * <p>
 * A snapshot that the reader rejects, or that the library refuses with a {@link CodegenException}, becomes a
 * {@link CodegenRefusedException}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
public class LibraryCodegenPort implements CodegenPort {

    @Override
    public List<GeneratedFile> generate(byte[] releaseJson, byte[] snapshotJson, String packageName,
                                        String projectName) {
        ContentRelease content;
        try {
            content = ContentReader.read(projectName, releaseJson, snapshotJson);
        } catch (ContentFormatException e) {
            throw new CodegenRefusedException(e.getMessage());
        }
        List<GeneratedSource> sources;
        try {
            sources = VulpesCodegen.generate(content, new CodegenOptions(packageName, CodegenTemplate.MINESTOM));
        } catch (CodegenException e) {
            throw new CodegenRefusedException(e.getMessage());
        }
        return sources.stream()
                .map(source -> new GeneratedFile(
                        source.packageName().replace('.', '/') + "/" + source.className() + ".java",
                        source.source()))
                .toList();
    }
}
