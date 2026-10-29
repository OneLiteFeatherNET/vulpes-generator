package net.onelitefeather.vulpes.generator.release;

/**
 * Writes the POM that is published next to every content JAR. The POM has no dependencies. Its comment
 * tells Gradle to read the {@code .module} file instead of the POM.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 1.0.0
 */
final class ContentPomWriter {

    private static final String TEMPLATE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <!-- do_not_remove: published-with-gradle-metadata -->
              <groupId>%s</groupId>
              <artifactId>%s</artifactId>
              <version>%s</version>
              <packaging>jar</packaging>
            </project>
            """;

    private ContentPomWriter() {
    }

    /**
     * Returns the POM text for the given coordinates. The output is deterministic.
     *
     * @param coordinates the coordinates of the content release
     * @return the POM as text with {@code \n} line endings
     */
    static String write(ContentCoordinates coordinates) {
        return TEMPLATE.formatted(coordinates.groupId(), coordinates.artifactId(), coordinates.version());
    }
}
