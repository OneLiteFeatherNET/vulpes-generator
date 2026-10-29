package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentCoordinatesTest {

    @Test
    void releaseCoordinatesUseTheContentGroup() {
        assertEquals("net.onelitefeather.vulpes.content", ContentCoordinates.forRelease("eldoria", "1.2.0").groupId(),
                "content artifacts must live in the content group");
    }

    @Test
    void toStringIsGroupArtifactVersion() {
        assertEquals("net.onelitefeather.vulpes.content:eldoria:1.2.0-dev.3",
                ContentCoordinates.forRelease("eldoria", "1.2.0-dev.3").toString(),
                "coordinates must render as group:artifact:version");
    }
}
