package net.theevilreaper.vulpes.generator.util;

import net.minestom.server.advancements.FrameType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FrameTypeParserTest {

    @Test
    void testInvalidFrameTypeParsing() {
        assertEquals(FrameType.TASK, FrameTypeParser.fromKey("invalid_type"), "Invalid frame type should return TASK");
    }

    @ParameterizedTest(name = "Test valid frame type parsing for: {0}")
    @EnumSource(FrameType.class)
    void testValidFrameTypeParsing(@NotNull FrameType frameType) {
        String key = frameType.name();
        assertEquals(frameType, FrameTypeParser.fromKey(key), "Parsed frame type should match the original");
    }

    @ParameterizedTest(name = "Test case-insensitive parsing for key: {0}")
    @ValueSource(strings = {"task", "Challenge", "GOAl"})
    void testCaseInsensitiveFrameTypeParsing(@NotNull String key) {
        FrameType expected = FrameTypeParser.fromKey(key.toUpperCase());
        assertEquals(expected, FrameTypeParser.fromKey(key), "Frame type parsing should be case insensitive");
    }

}