package net.onelitefeather.vulpes.generator.generation.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.item.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterialCodecTest {

    @Test
    void decodesKnownMaterialKey() {
        Result<Material> result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive("minecraft:stone"));

        assertEquals(Material.STONE, result.orElseThrow(), "The key minecraft:stone should decode to STONE");
    }

    @Test
    void decodesKeyWithoutNamespaceToMinecraftMaterial() {
        Result<Material> result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive("stone"));

        assertEquals(Material.STONE, result.orElseThrow(), "A key without namespace should resolve in the minecraft namespace");
    }

    @Test
    void rejectsUnknownMaterialKeyWithDescriptiveMessage() {
        Result<Material> result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive("minecraft:not_a_material"));

        Result.Error<Material> error = assertInstanceOf(Result.Error.class, result, "An unknown key must produce an error result");
        assertTrue(error.message().contains("minecraft:not_a_material"), "The error should name the unknown key but was: " + error.message());
    }

    @Test
    void rejectsEmptyMaterialKey() {
        Result<Material> result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive(""));

        assertInstanceOf(Result.Error.class, result, "An empty key must produce an error result instead of a fallback material");
    }

    @Test
    void doesNotFallBackToStoneForUnknownKey() {
        Result<Material> result = MaterialCodec.CODEC.decode(Transcoder.JSON, new JsonPrimitive("minecraft:not_a_material"));

        assertInstanceOf(Result.Error.class, result, "An unknown key must not silently decode to STONE");
    }

    @Test
    void encodesMaterialAsNamespacedKey() {
        Result<JsonElement> result = MaterialCodec.CODEC.encode(Transcoder.JSON, Material.STONE);

        assertEquals(new JsonPrimitive("minecraft:stone"), result.orElseThrow(), "STONE should be encoded as its namespaced key");
    }
}
