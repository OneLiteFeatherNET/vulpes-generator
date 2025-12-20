package net.onelitefeather.vulpes.generator.gson.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiFunction;

/**
 * This class is used to serialize and deserialize a {@link Key} object.
 * The {@link Key} object is represented as a JSON object with the keys "namespace" and "value".
 * The namespace is the namespace of the key and the value is the value of the key.
 * The JSON object is represented as follows:
 * <pre>
 *     {
 *         "namespace": "namespace",
 *         "value": "value"
 *     }
 * </pre>
 * A key can be created by calling the {@link Key#key(String, String)} method.
 * Use {@link #create()} to create a default adapter.
 * Use {@link #create(BiFunction)} to create an adapter with a custom key creation function.
 * Use {@link #createMinestom()} to create an adapter with a custom key({@link Key}) creation function for Minestom.
 *
 * @since 1.0.0
 * @version 1.0.0
 * @see Key
 * @see Key#key(String, String)
 * @author TheMeinerLP
 */
public class KeyGsonAdapter extends TypeAdapter<Key> {

    private final BiFunction<String, String, Key> createKeyObject;

    private KeyGsonAdapter(@NotNull BiFunction<String, String, Key> createKeyObject) {
        this.createKeyObject = createKeyObject;
    }

    private KeyGsonAdapter() {
        this(Key::key);
    }

    /**
     * Creates a new instance of the {@link KeyGsonAdapter} with the default key creation function.
     * @return the new instance of the {@link KeyGsonAdapter}
     */
    @Contract(value = " -> new", pure = true)
    public static @NotNull KeyGsonAdapter create() {
        return new KeyGsonAdapter();
    }

    /**
     * Creates a new instance of the {@link KeyGsonAdapter} with a custom key creation function.
     * @param createKeyObject the custom key creation function
     * @return the new instance of the {@link KeyGsonAdapter}
     */
    @Contract(value = "_ -> new", pure = true)
    public static @NotNull KeyGsonAdapter create(@NotNull BiFunction<String, String, Key> createKeyObject) {
        return new KeyGsonAdapter(createKeyObject);
    }

    /**
     * Creates a new instance of the {@link KeyGsonAdapter} with a custom key) creation function for Minestom.
     * @return the new instance of the {@link KeyGsonAdapter}
     */
    public static @NotNull KeyGsonAdapter createMinestom() {
        return new KeyGsonAdapter(Key::key);
    }

    @Override
    public void write(JsonWriter out, Key value) throws IOException {
        if (value == null) {
            return;
        }
        out.beginObject();
        out.name("namespace").value(value.namespace());
        out.name("value").value(value.value());
        out.endObject();
    }

    @Override
    public Key read(JsonReader in) throws IOException {
        in.beginObject();
        if (!in.nextName().equals("namespace")) {
            throw new IOException("Expected namespace");
        }
        var namespace = in.nextString();
        if (!in.nextName().equals("value")) {
            throw new IOException("Expected value");
        }
        var value = in.nextString();
        in.endObject();
        return this.createKeyObject.apply(namespace, value);
    }
}
