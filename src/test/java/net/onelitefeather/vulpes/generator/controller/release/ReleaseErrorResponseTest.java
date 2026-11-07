package net.onelitefeather.vulpes.generator.controller.release;

import io.micronaut.serde.ObjectMapper;
import net.onelitefeather.vulpes.generator.release.ReleaseDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the JSON body of a refused release, as the Stelaris client reads it.
 *
 * @author TheMeinerLP
 */
class ReleaseErrorResponseTest {

    @Test
    void errorWithoutDiagnosticsOmitsTheFields() throws Exception {
        String json = ObjectMapper.create(Map.of()).writeValueAsString(
                new ReleaseErrorResponse("VERSION_EXISTS", "version 2.0.0 exists"));

        assertEquals("{\"reason\":\"VERSION_EXISTS\",\"message\":\"version 2.0.0 exists\"}", json,
                "diagnostics and truncated must be absent from the body of an error without them");
    }

    @Test
    void diagnosticsAndTruncationAreWrittenAtTheTopLevel() throws Exception {
        ReleaseDiagnostic diagnostic = new ReleaseDiagnostic("items/Sword.java", 12, "cannot find symbol");

        String json = ObjectMapper.create(Map.of()).writeValueAsString(
                new ReleaseErrorResponse("COMPILATION_FAILED", "2 errors", List.of(diagnostic), true));

        assertEquals("{\"reason\":\"COMPILATION_FAILED\",\"message\":\"2 errors\","
                        + "\"diagnostics\":[{\"file\":\"items/Sword.java\",\"line\":12,\"message\":\"cannot find symbol\"}],"
                        + "\"truncated\":true}", json,
                "diagnostics and truncated must be top-level fields of the error body");
    }
}
