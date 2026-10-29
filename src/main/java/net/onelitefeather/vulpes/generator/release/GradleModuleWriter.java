package net.onelitefeather.vulpes.generator.release;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Writes the Gradle module metadata ({@code .module}) of a content JAR. It describes the JAR for the
 * {@code apiElements} and {@code runtimeElements} variants and carries its size and checksums.
 * The output is deterministic: keys are written in a fixed order with two-space indentation.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class GradleModuleWriter {

    private static final String INDENT = "  ";

    private GradleModuleWriter() {
    }

    /**
     * Returns the module metadata for one JAR.
     *
     * @param coordinates the coordinates of the content release
     * @param jarFileName the file name of the JAR, used as file name and URL of the variant files
     * @param jar         the JAR bytes, used for the size and the checksums
     * @return the module metadata as text with {@code \n} line endings
     */
    static String write(ContentCoordinates coordinates, String jarFileName, byte[] jar) {
        StringBuilder json = new StringBuilder();
        line(json, 0, "{");
        line(json, 1, "\"formatVersion\": \"1.1\",");
        line(json, 1, "\"component\": {");
        line(json, 2, "\"group\": " + quote(coordinates.groupId()) + ",");
        line(json, 2, "\"module\": " + quote(coordinates.artifactId()) + ",");
        line(json, 2, "\"version\": " + quote(coordinates.version()));
        line(json, 1, "},");
        line(json, 1, "\"variants\": [");
        variant(json, "apiElements", "java-api", jarFileName, jar, true);
        variant(json, "runtimeElements", "java-runtime", jarFileName, jar, false);
        line(json, 1, "]");
        line(json, 0, "}");
        return json.toString();
    }

    private static void variant(StringBuilder json, String name, String usage,
                                String jarFileName, byte[] jar, boolean first) {
        line(json, 2, "{");
        line(json, 3, "\"name\": " + quote(name) + ",");
        line(json, 3, "\"attributes\": {");
        line(json, 4, "\"org.gradle.category\": \"library\",");
        line(json, 4, "\"org.gradle.dependency.bundling\": \"external\",");
        line(json, 4, "\"org.gradle.libraryelements\": \"jar\",");
        line(json, 4, "\"org.gradle.usage\": " + quote(usage));
        line(json, 3, "},");
        line(json, 3, "\"files\": [");
        line(json, 4, "{");
        line(json, 5, "\"name\": " + quote(jarFileName) + ",");
        line(json, 5, "\"url\": " + quote(jarFileName) + ",");
        line(json, 5, "\"size\": " + jar.length + ",");
        line(json, 5, "\"sha512\": " + quote(digest("SHA-512", jar)) + ",");
        line(json, 5, "\"sha256\": " + quote(digest("SHA-256", jar)) + ",");
        line(json, 5, "\"sha1\": " + quote(digest("SHA-1", jar)) + ",");
        line(json, 5, "\"md5\": " + quote(digest("MD5", jar)));
        line(json, 4, "}");
        line(json, 3, "]");
        line(json, 2, first ? "}," : "}");
    }

    private static void line(StringBuilder json, int depth, String text) {
        json.append(INDENT.repeat(depth)).append(text).append('\n');
    }

    /** Quotes a value. The values are validated Maven coordinates and file names, so no escaping is needed. */
    private static String quote(String value) {
        return "\"" + value + "\"";
    }

    private static String digest(String algorithm, byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is required by every Java runtime", e);
        }
    }
}
