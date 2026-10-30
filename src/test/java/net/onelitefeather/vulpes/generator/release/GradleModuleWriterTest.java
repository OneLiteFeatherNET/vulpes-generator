package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the Gradle module metadata written next to every content JAR.
 *
 * @author TheMeinerLP
 */
class GradleModuleWriterTest {

    private static final byte[] JAR = "jar-bytes".getBytes(StandardCharsets.UTF_8);

    @Test
    void writesTheExactModuleMetadataForOneJar() {
        String expected = """
                {
                  "formatVersion": "1.1",
                  "component": {
                    "group": "net.onelitefeather.vulpes.content",
                    "module": "eldoria",
                    "version": "1.2.0"
                  },
                  "variants": [
                    {
                      "name": "apiElements",
                      "attributes": {
                        "org.gradle.category": "library",
                        "org.gradle.dependency.bundling": "external",
                        "org.gradle.libraryelements": "jar",
                        "org.gradle.usage": "java-api"
                      },
                      "files": [
                        {
                          "name": "eldoria-1.2.0.jar",
                          "url": "eldoria-1.2.0.jar",
                          "size": 9,
                          "sha512": "6125056b9690c4b7556d1a5ac31e58756985a1843c472a9a2d36b55f4b8a6fda3dac95d5007060e71c51353a4c9c48b5af8e2d56ea593e87427f674bce7ccc8f",
                          "sha256": "829b21a069ff177599d32249ba84e0979b39f7fcba8a437607be0b9b06b51c20",
                          "sha1": "04e2ebe8b7b182c63c2834f4984aae2901150df1",
                          "md5": "e9849ed68094501437b5bd6aa4aecd34"
                        }
                      ]
                    },
                    {
                      "name": "runtimeElements",
                      "attributes": {
                        "org.gradle.category": "library",
                        "org.gradle.dependency.bundling": "external",
                        "org.gradle.libraryelements": "jar",
                        "org.gradle.usage": "java-runtime"
                      },
                      "files": [
                        {
                          "name": "eldoria-1.2.0.jar",
                          "url": "eldoria-1.2.0.jar",
                          "size": 9,
                          "sha512": "6125056b9690c4b7556d1a5ac31e58756985a1843c472a9a2d36b55f4b8a6fda3dac95d5007060e71c51353a4c9c48b5af8e2d56ea593e87427f674bce7ccc8f",
                          "sha256": "829b21a069ff177599d32249ba84e0979b39f7fcba8a437607be0b9b06b51c20",
                          "sha1": "04e2ebe8b7b182c63c2834f4984aae2901150df1",
                          "md5": "e9849ed68094501437b5bd6aa4aecd34"
                        }
                      ]
                    }
                  ]
                }
                """;

        String module = GradleModuleWriter.write(ContentCoordinates.forRelease("eldoria", "1.2.0"), "eldoria-1.2.0.jar", JAR);

        assertEquals(expected, module, "the module must describe both variants with the jar and its checksums");
    }
}
