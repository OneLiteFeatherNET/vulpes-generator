package net.onelitefeather.vulpes.generator.generation.font;

import com.google.gson.JsonElement;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Codec for a Minecraft {@code bitmap} font provider, for example:
 * <pre>{@code
 * {"type": "bitmap", "file": "eldoria:font/hud_icons.png", "ascent": 7, "height": 8, "chars": ["ab", "cd"]}
 * }</pre>
 * <p>
 * The {@code height} is optional and defaults to {@link #DEFAULT_HEIGHT}. All rows of {@code chars} must have the same
 * length in codepoints, which is a vanilla rule for bitmap providers.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@ApiStatus.Internal
public final class BitmapFontProviderCodec {

    /**
     * The provider type which is the only one this codec accepts.
     */
    public static final String TYPE = "bitmap";

    /**
     * The height which is used when a font has no height.
     */
    public static final int DEFAULT_HEIGHT = 8;

    private static final Pattern FILE_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+\\.png");

    /**
     * A validated bitmap font provider.
     *
     * @param file   the texture as {@code namespace:path}, always ending in {@code .png}
     * @param ascent the ascent of the glyphs, not greater than the height
     * @param height the height of the glyphs
     * @param chars  the rows of glyphs, never empty, every row has the same codepoint count
     */
    public record BitmapFontProvider(String file, int ascent, int height, List<String> chars) {

        /**
         * Validates the provider and copies the rows.
         *
         * @throws IllegalArgumentException if the file, the ascent, the height or the rows are invalid
         */
        public BitmapFontProvider {
            Objects.requireNonNull(file, "file must not be null");
            if (!FILE_PATTERN.matcher(file).matches()) {
                throw new IllegalArgumentException("file must be a namespace:path key ending in .png but was '" + file + "'");
            }
            if (ascent > height) {
                throw new IllegalArgumentException("ascent must not be greater than height but was ascent " + ascent + " and height " + height);
            }
            if (chars == null || chars.isEmpty()) {
                throw new IllegalArgumentException("chars must not be empty");
            }
            chars = List.copyOf(chars);

            int width = -1;
            for (String row : chars) {
                if (row.isEmpty()) {
                    throw new IllegalArgumentException("chars must not contain an empty row");
                }
                int length = row.codePointCount(0, row.length());
                if (width == -1) {
                    width = length;
                } else if (length != width) {
                    throw new IllegalArgumentException("all chars rows must have the same length in codepoints, but a row has " + length + " and the first row has " + width);
                }
            }
        }
    }

    /**
     * The codec which maps a bitmap provider to its JSON object and back.
     * The {@code type} is always written as {@value #TYPE} and every other type is rejected while decoding.
     */
    public static final StructCodec<BitmapFontProvider> CODEC = StructCodec.struct(
            "type", Codec.STRING.transform(BitmapFontProviderCodec::checkType, BitmapFontProviderCodec::checkType),
            provider -> TYPE,
            "file", Codec.STRING, BitmapFontProvider::file,
            "ascent", Codec.INT, BitmapFontProvider::ascent,
            "height", Codec.INT.optional(DEFAULT_HEIGHT), BitmapFontProvider::height,
            "chars", Codec.STRING.list(), BitmapFontProvider::chars,
            (type, file, ascent, height, chars) -> new BitmapFontProvider(file, ascent, height, chars)
    );

    /**
     * Encodes a provider to a JSON object.
     *
     * @param provider the provider to encode
     * @return the JSON object of the provider
     * @throws IllegalStateException if the codec cannot encode the provider
     */
    public static JsonElement toJson(BitmapFontProvider provider) {
        Result<JsonElement> result = CODEC.encode(Transcoder.JSON, provider);
        if (result instanceof Result.Ok<JsonElement> ok) {
            return ok.value();
        }
        String reason = result instanceof Result.Error<JsonElement> error ? error.message() : "no value encoded";
        throw new IllegalStateException("unable to encode bitmap font provider for " + provider.file() + ": " + reason);
    }

    private static String checkType(String type) {
        if (!TYPE.equals(type)) {
            throw new IllegalArgumentException("type must be '" + TYPE + "' but was '" + type + "'");
        }
        return type;
    }

    private BitmapFontProviderCodec() {
        // Utility class, no instances
    }
}
