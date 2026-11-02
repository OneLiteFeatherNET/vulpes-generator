package net.onelitefeather.vulpes.generator.release;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static net.onelitefeather.vulpes.generator.release.ProjectFixture.json;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the canonical encoding that makes the snapshot bytes, and therefore the revision, reproducible.
 *
 * @author TheMeinerLP
 */
class CanonicalJsonTest {

    @Test
    void objectKeysAreSortedAtEveryLevel() {
        String text = encode("{\"b\":{\"d\":1,\"c\":2},\"a\":3}");

        assertEquals("{\"a\":3,\"b\":{\"c\":2,\"d\":1}}", text, "object keys must be sorted at every level");
    }

    @Test
    void outputHasNoInsignificantWhitespace() {
        String text = encode("{ \"a\" : [ 1 , 2 ] , \"b\" : \"x y\" }");

        assertEquals("{\"a\":[1,2],\"b\":\"x y\"}", text, "the output must not contain whitespace outside strings");
    }

    @Test
    void arrayOrderIsKept() {
        String text = encode("{\"list\":[\"z\",\"a\"]}");

        assertEquals("{\"list\":[\"z\",\"a\"]}", text, "arrays are sorted by the mapper, the writer must keep their order");
    }

    @Test
    void nonAsciiCharactersAreWrittenAsUtf8NotEscaped() {
        byte[] bytes = CanonicalJson.write(json("{\"name\":\"Jörg\"}"));

        assertEquals("{\"name\":\"Jörg\"}", new String(bytes, StandardCharsets.UTF_8),
                "non-ASCII characters must be written as UTF-8, not as escape sequences");
    }

    @Test
    void numbersKeepTheirValueAndDecimalPoint() {
        String text = encode("{\"d\":1.0,\"n\":-1,\"f\":0.2}");

        assertEquals("{\"d\":1.0,\"f\":0.2,\"n\":-1}", text, "numbers must keep their value and their written form");
    }

    private static String encode(String json) {
        return new String(CanonicalJson.write(json(json)), StandardCharsets.UTF_8);
    }
}
