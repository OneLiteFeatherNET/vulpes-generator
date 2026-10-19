package net.onelitefeather.vulpes.generator.generation;

import org.jetbrains.annotations.NotNull;

import javax.lang.model.SourceVersion;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Turns keys of the database into Java identifiers that compile.
 * <p>
 * Keys may contain characters that are illegal in Java, such as {@code -} or {@code .}, and may start with a digit.
 * The helpers replace these characters, collapse repeated underscores and append or prepend an underscore where the
 * Java language requires it. Two keys that end up with the same identifier are rejected with an
 * {@link IllegalArgumentException} that names both keys, nothing is dropped silently.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
public final class JavaIdentifiers {

    private static final String UNDERSCORE = "_";

    private JavaIdentifiers() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Returns a constant name for the given key, for example {@code my-item.v2} becomes {@code MY_ITEM_V2}.
     *
     * @param key the key to convert
     * @return a valid Java identifier in upper case
     * @throws IllegalArgumentException if the key contains no letter or digit
     */
    public static @NotNull String constantName(@NotNull String key) {
        return toIdentifier(key.toUpperCase(Locale.ROOT), key);
    }

    /**
     * Returns a local variable name made of the prefix and the key in lower case, for example
     * {@code localName("adv_", "My-Item")} returns {@code adv_my_item}.
     *
     * @param prefix the prefix of the name, may be empty
     * @param key    the key to convert
     * @return a valid Java identifier in lower case
     * @throws IllegalArgumentException if the key contains no letter or digit
     */
    public static @NotNull String localName(@NotNull String prefix, @NotNull String key) {
        return toIdentifier(prefix + key.toLowerCase(Locale.ROOT), key);
    }

    private static @NotNull String toIdentifier(@NotNull String text, @NotNull String key) {
        String identifier = text.replaceAll("[^A-Za-z0-9_]", UNDERSCORE).replaceAll("_+", UNDERSCORE);
        if (identifier.replace(UNDERSCORE, "").isEmpty()) {
            throw new IllegalArgumentException("key '" + key + "' contains no letter or digit and cannot become a Java identifier");
        }
        if (Character.isDigit(identifier.charAt(0))) {
            identifier = UNDERSCORE + identifier;
        }
        if (SourceVersion.isKeyword(identifier)) {
            identifier = identifier + UNDERSCORE;
        }
        return identifier;
    }

    /**
     * Tracks the identifiers of one class or method and rejects two keys that map to the same identifier.
     */
    public static final class Claims {

        private final Map<String, String> keyByIdentifier = new HashMap<>();

        /**
         * Claims the identifier for the given key.
         *
         * @param identifier the identifier created from the key
         * @param key        the key that owns the identifier
         * @return the identifier
         * @throws IllegalArgumentException if another key already owns the identifier
         */
        public @NotNull String claim(@NotNull String identifier, @NotNull String key) {
            String owner = keyByIdentifier.putIfAbsent(identifier, key);
            if (owner != null && !owner.equals(key)) {
                throw new IllegalArgumentException("keys '" + owner + "' and '" + key
                        + "' both map to the Java identifier '" + identifier + "'");
            }
            return identifier;
        }
    }
}
