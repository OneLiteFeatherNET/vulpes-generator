package net.onelitefeather.vulpes.generator.util;

import org.jetbrains.annotations.ApiStatus;

/**
 * Constants used in the application.
 */
@ApiStatus.Internal
public final class Constants {

    public static final String INDENT_DEFAULT = "   ";

    public static final String TEMP_PREFIX = "vulpes-";
    public static final String ZIP_FILE_NAME = "gradle_template.zip";

    public static final String GITLAB_CI_YML = ".gitlab-ci.yml";

    public static final String GRADLE_PROPERTIES = "gradle.properties";
    public static final String OUT_PUT_FOLDER = "out";
    public static final String JAVA_MAIM_FOLDER = "src/main/java";

    public static final String EMPTY_STRING = "";

    private Constants() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
