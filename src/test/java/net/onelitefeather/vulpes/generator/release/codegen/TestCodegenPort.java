package net.onelitefeather.vulpes.generator.release.codegen;

import jakarta.inject.Singleton;

import java.util.List;

/**
 * The code generator bean of every test context until the adapter to the codegen library is provided. It generates
 * nothing; unit tests that need generated sources use their own fake.
 *
 * @author TheMeinerLP
 */
@Singleton
class TestCodegenPort implements CodegenPort {

    @Override
    public List<GeneratedFile> generate(byte[] releaseJson, byte[] snapshotJson, String packageName,
                                        String projectName) {
        return List.of();
    }
}
