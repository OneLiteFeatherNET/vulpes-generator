package net.onelitefeather.vulpes.generator.release.codegen;

import java.util.List;

/**
 * Port to the code generator of the Vulpes Gradle plugin. The generator turns a snapshot into Java sources for one
 * project. The real adapter to the published {@code vulpes-codegen} library is provided by a later change; the tests
 * use a fake.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface CodegenPort {

    /**
     * Generates the Java sources of one project.
     *
     * @param releaseJson  the {@code release.json} of the release, format 2, as bytes
     * @param snapshotJson the snapshot of the project, as bytes
     * @param packageName  the Java package the sources are generated into
     * @param projectName  the project name of the snapshot
     * @return the generated files, each with a path relative to the generated root
     * @throws CodegenRefusedException when the generator refuses the snapshot content
     */
    List<GeneratedFile> generate(byte[] releaseJson, byte[] snapshotJson, String packageName, String projectName);
}
