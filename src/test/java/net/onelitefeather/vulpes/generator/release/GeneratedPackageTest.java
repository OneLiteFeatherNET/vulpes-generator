package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the default Java package of a project, which must match the default of the Vulpes Gradle plugin.
 *
 * @author TheMeinerLP
 */
class GeneratedPackageTest {

    @Test
    void plainProjectKeyIsThePackageSegment() {
        assertEquals("net.onelitefeather.vulpes.content.eldoria", GeneratedPackage.of("eldoria"),
                "a plain key must be the last segment of the default package");
    }

    @Test
    void hyphenIsReplacedByUnderscore() {
        assertEquals("net.onelitefeather.vulpes.content.my_game", GeneratedPackage.of("my-game"),
                "a hyphen is not allowed in a package name");
    }

    @Test
    void javaKeywordGetsAnUnderscoreSuffix() {
        assertEquals("net.onelitefeather.vulpes.content.package_", GeneratedPackage.of("package"),
                "a keyword cannot be a package segment");
    }
}
