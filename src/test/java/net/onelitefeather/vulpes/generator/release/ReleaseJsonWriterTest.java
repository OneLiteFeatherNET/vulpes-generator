package net.onelitefeather.vulpes.generator.release;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static net.onelitefeather.vulpes.generator.release.ReleaseTestData.PROJECT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the release package.
 *
 * @author TheMeinerLP
 */
class ReleaseJsonWriterTest {

    private final ReleaseJsonWriter writer = new ReleaseJsonWriter();

    @Test
    void writesExactCanonicalBytesForOneRequest() {
        ReleaseRequest request = ReleaseTestData.request();
        String expected = "{\"format\":1,\"packs\":[],\"project\":\"eldoria\",\"projectId\":\"" + PROJECT_ID
                + "\",\"provenance\":{\"createdAt\":\"2026-10-28T12:00:00Z\",\"createdBy\":\"theEvilReaper\","
                + "\"generator\":\"1.0.0\"},\"revision\":\"" + request.revision()
                + "\",\"version\":\"1.2.0\"}";

        byte[] json = writer.write(request, "1.0.0");

        assertEquals(expected, new String(json, StandardCharsets.UTF_8),
                "release.json must use sorted keys, empty packs and no insignificant whitespace");
    }

    @Test
    void createdAtIsTakenFromTheRequest() {
        ReleaseRequest request = new ReleaseRequest(PROJECT_ID, "eldoria", "1.2.0",
                ReleaseTestData.request().revision(), "theEvilReaper", Instant.parse("2001-02-03T04:05:06Z"));

        String json = text(writer.write(request, "1.0.0"));

        assertEquals("2001-02-03T04:05:06Z", provenance(json).get("createdAt").asText(),
                "provenance.createdAt must be the request value, not the clock");
    }

    @Test
    void generatorIsTakenFromTheGeneratorVersion() {
        String json = text(writer.write(ReleaseTestData.request(), "7.8.9"));

        assertEquals("7.8.9", provenance(json).get("generator").asText(),
                "provenance.generator must be the generator version passed in");
    }

    @Test
    void packsIsAnEmptyArray() {
        JsonNode root = parse(writer.write(ReleaseTestData.request(), "1.0.0"));

        assertTrue(root.get("packs").isArray(), "packs must be an array");
        assertEquals(0, root.get("packs").size(), "packs must be an empty array in this change");
    }

    @Test
    void formatIsOne() {
        JsonNode root = parse(writer.write(ReleaseTestData.request(), "1.0.0"));

        assertEquals(1, root.get("format").asInt(), "release format must be 1");
    }

    @Test
    void specialCharactersInStringsAreEscaped() {
        ReleaseRequest request = new ReleaseRequest(PROJECT_ID, "eldoria", "1.2.0",
                ReleaseTestData.request().revision(), "quote\" back\\slash\nnewline\u0001", Instant.EPOCH);

        String json = text(writer.write(request, "1.0.0"));

        assertEquals("quote\" back\\slash\nnewline\u0001", provenance(json).get("createdBy").asText(),
                "strings must round-trip through a standard JSON parser");
    }

    @Test
    void nonAsciiCharactersAreWrittenAsUtf8() {
        ReleaseRequest request = new ReleaseRequest(PROJECT_ID, "eldoria", "1.2.0",
                ReleaseTestData.request().revision(), "Jörg", Instant.EPOCH);

        String json = new String(writer.write(request, "1.0.0"), StandardCharsets.UTF_8);

        assertEquals("Jörg", provenance(json).get("createdBy").asText(), "non-ASCII must survive UTF-8 encoding");
    }

    private static String text(byte[] json) {
        return new String(json, StandardCharsets.UTF_8);
    }

    private static JsonNode provenance(String json) {
        return parse(json.getBytes(StandardCharsets.UTF_8)).get("provenance");
    }

    private static JsonNode parse(byte[] json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new AssertionError("release.json must be valid JSON", e);
        }
    }

    private static JsonNode parse(String json) {
        return parse(json.getBytes(StandardCharsets.UTF_8));
    }
}
