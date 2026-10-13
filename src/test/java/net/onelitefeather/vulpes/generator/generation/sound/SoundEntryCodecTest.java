package net.onelitefeather.vulpes.generator.generation.sound;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundEntryCodecTest {

    @Test
    void encodesEntryWithOnlyDefaultValuesAsPlainName() {
        Result<JsonElement> result = SoundEntryCodec.CODEC.encode(Transcoder.JSON, defaults("eldoria/click"));

        assertEquals(new JsonPrimitive("eldoria/click"), result.orElseThrow(),
                "An entry with only Vanilla default values must be written as its plain name");
    }

    @Test
    void omitsDefaultFieldsWhenEncodingEntry() {
        SoundEntry entry = new SoundEntry("eldoria/click", 0.5f, 1f, 1, false, 16, false, "file");

        JsonObject object = encodeObject(entry);

        assertEquals(Set.of("name", "volume"), object.keySet(),
                "Only the name and the non-default volume must be written but was " + object);
    }

    @Test
    void writesEveryNonDefaultFieldWithItsJsonName() {
        SoundEntry entry = new SoundEntry("eldoria:ui.click", 0.5f, 1.5f, 3, true, 32, true, "event");

        JsonObject object = encodeObject(entry);

        assertEquals(Set.of("name", "volume", "pitch", "weight", "stream", "attenuation_distance", "preload", "type"),
                object.keySet(), "Every non-default field must use its sounds.json name but was " + object);
    }

    @Test
    void decodesPlainNameWithDefaultValues() {
        Result<SoundEntry> result = SoundEntryCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive("eldoria/click"));

        assertEquals(defaults("eldoria/click"), result.orElseThrow(),
                "A plain name must decode to an entry with the Vanilla defaults");
    }

    @Test
    void decodesMissingFieldsWithDefaultValues() {
        Result<SoundEntry> result = SoundEntryCodec.CODEC.decode(Transcoder.JSON,
                JsonParser.parseString("{\"name\":\"eldoria/click\",\"volume\":0.5}"));

        assertEquals(new SoundEntry("eldoria/click", 0.5f, 1f, 1, false, 16, false, "file"), result.orElseThrow(),
                "Fields missing from the object must fall back to the Vanilla defaults");
    }

    @Test
    void normalizesLegacyTypeFileToFile() {
        Result<SoundEntry> result = SoundEntryCodec.CODEC.decode(Transcoder.JSON,
                JsonParser.parseString("{\"name\":\"eldoria/click\",\"volume\":0.5,\"type\":\"type_file\"}"));

        assertEquals("file", result.orElseThrow().type(), "The legacy type_file value must be read as file");
    }

    @ParameterizedTest(name = "Entry {0} should be rejected")
    @ValueSource(strings = {
            "{\"name\":\"\"}",
            "{\"name\":\"eldoria/click\",\"volume\":0}",
            "{\"name\":\"eldoria/click\",\"pitch\":-1}",
            "{\"name\":\"eldoria/click\",\"weight\":0}",
            "{\"name\":\"eldoria/click\",\"attenuation_distance\":0}",
            "{\"name\":\"eldoria/click\",\"type\":\"music\"}"
    })
    void rejectsInvalidEntryWhenDecoding(String json) {
        Result<SoundEntry> result = SoundEntryCodec.CODEC.decode(Transcoder.JSON, JsonParser.parseString(json));

        assertInstanceOf(Result.Error.class, result, "An entry with an invalid value must produce an error result");
    }

    @Test
    void rejectsEncodingEntryWithNonPositiveVolume() {
        Result<JsonElement> result = SoundEntryCodec.CODEC.encode(Transcoder.JSON,
                new SoundEntry("eldoria/click", 0f, 1f, 1, false, 16, false, "file"));

        assertInstanceOf(Result.Error.class, result, "A volume of 0 must not be encodable");
    }

    private static SoundEntry defaults(String name) {
        return new SoundEntry(name, 1f, 1f, 1, false, 16, false, "file");
    }

    private static JsonObject encodeObject(SoundEntry entry) {
        JsonElement json = SoundEntryCodec.CODEC.encode(Transcoder.JSON, entry).orElseThrow();
        assertTrue(json.isJsonObject(), "An entry with a non-default field must be written as an object but was " + json);
        return json.getAsJsonObject();
    }
}
