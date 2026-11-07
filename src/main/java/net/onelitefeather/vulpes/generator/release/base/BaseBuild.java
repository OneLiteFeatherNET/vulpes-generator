package net.onelitefeather.vulpes.generator.release.base;

import java.nio.file.Path;

/**
 * Port for building one checkout of {@code vulpes-base}. The implementation writes the built jar and the dependency
 * jars into the export directory, and nothing else.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public interface BaseBuild {

    /**
     * Builds the checkout and exports the jar and the classpath.
     *
     * @param checkout the checkout of one commit of {@code vulpes-base}
     * @param export   the empty directory that receives {@code vulpes-base.jar} and {@code lib/}
     * @throws net.onelitefeather.vulpes.generator.release.ReleaseRejectedException with {@code BASE_UNAVAILABLE} when
     *                                  the build fails, exceeds its time limit or exports no jar
     */
    void build(Path checkout, Path export);
}
