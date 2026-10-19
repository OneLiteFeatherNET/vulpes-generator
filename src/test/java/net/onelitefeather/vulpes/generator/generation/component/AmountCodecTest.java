package net.onelitefeather.vulpes.generator.generation.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AmountCodecTest {

    @ParameterizedTest(name = "Amount {0} should be decoded")
    @ValueSource(ints = {1, 2, 42, 99})
    void decodesAmountWithinRange(int amount) {
        Result<Integer> result = AmountCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive(amount));

        assertEquals(amount, result.orElseThrow(), "An amount inside 1..99 should be decoded unchanged");
    }

    @ParameterizedTest(name = "Amount {0} should be rejected")
    @ValueSource(ints = {0, -1, 100, 1000})
    void rejectsAmountOutsideRange(int amount) {
        Result<Integer> result = AmountCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive(amount));

        assertInstanceOf(Result.Error.class, result, "An amount outside 1..99 should produce an error result");
    }

    @ParameterizedTest(name = "Amount {0} should be encoded")
    @ValueSource(ints = {1, 99})
    void encodesAmountWithinRange(int amount) {
        Result<JsonElement> result = AmountCodec.CODEC.encode(Transcoder.JSON, amount);

        assertEquals(new JsonPrimitive(amount), result.orElseThrow(), "An amount inside 1..99 should be encoded as a JSON number");
    }

    @ParameterizedTest(name = "Amount {0} should not be encoded")
    @ValueSource(ints = {0, 100})
    void rejectsEncodingAmountOutsideRange(int amount) {
        Result<JsonElement> result = AmountCodec.CODEC.encode(Transcoder.JSON, amount);

        assertInstanceOf(Result.Error.class, result, "An amount outside 1..99 should not be encodable");
    }
}
