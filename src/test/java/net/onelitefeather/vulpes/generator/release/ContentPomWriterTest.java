package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the POM written next to every content JAR.
 *
 * @author TheMeinerLP
 */
class ContentPomWriterTest {

    @Test
    void writesTheExactPomForOneCoordinateSet() {
        String expected = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <!-- do_not_remove: published-with-gradle-metadata -->
                  <groupId>net.onelitefeather.vulpes.content</groupId>
                  <artifactId>eldoria</artifactId>
                  <version>1.2.0</version>
                  <packaging>jar</packaging>
                </project>
                """;

        String pom = ContentPomWriter.write(ContentCoordinates.forRelease("eldoria", "1.2.0"));

        assertEquals(expected, pom, "the POM must carry the coordinates, no dependencies and the Gradle marker");
    }
}
