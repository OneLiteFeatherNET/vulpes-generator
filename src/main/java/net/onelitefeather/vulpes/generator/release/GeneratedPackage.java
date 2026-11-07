package net.onelitefeather.vulpes.generator.release;

import javax.lang.model.SourceVersion;

/**
 * The Java package of the sources generated for a project. It is the default package of the Vulpes Gradle plugin:
 * {@code net.onelitefeather.vulpes.content.} followed by the project key with {@code -} replaced by {@code _}, and
 * {@code _} appended when the result is a Java keyword.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
public final class GeneratedPackage {

    /** The prefix of the default package of a content project. */
    static final String PREFIX = "net.onelitefeather.vulpes.content.";

    private GeneratedPackage() {
    }

    /**
     * Returns the default package of a project.
     *
     * @param projectKey the project key, for example {@code my-game}
     * @return the package, for example {@code net.onelitefeather.vulpes.content.my_game}
     */
    public static String of(String projectKey) {
        String segment = projectKey.replace('-', '_');
        return PREFIX + (SourceVersion.isKeyword(segment) ? segment + "_" : segment);
    }
}
