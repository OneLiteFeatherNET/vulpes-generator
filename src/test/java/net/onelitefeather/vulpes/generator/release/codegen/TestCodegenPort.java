package net.onelitefeather.vulpes.generator.release.codegen;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * The code generator bean of the test contexts. It replaces the production {@link LibraryCodegenPort}, so that the
 * Micronaut tests never run the generator; it generates nothing. Unit tests that need generated sources use their own
 * fake, and {@code LibraryCodegenPortTest} covers the adapter.
 *
 * @author TheMeinerLP
 */
@Singleton
@Replaces(LibraryCodegenPort.class)
class TestCodegenPort implements CodegenPort {

    @Override
    public List<GeneratedFile> generate(byte[] releaseJson, byte[] snapshotJson, String packageName,
                                        String projectName) {
        return List.of();
    }
}
