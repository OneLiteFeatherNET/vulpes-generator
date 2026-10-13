package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.onelitefeather.vulpes.api.model.sound.SoundEventEntity;
import net.onelitefeather.vulpes.api.repository.SoundRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventDefinition;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventDefinitionCodec;
import net.onelitefeather.vulpes.generator.generation.sound.SoundEventKey;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Generates one {@code sounds.json} per namespace at {@code assets/<namespace>/sounds.json} below the given path.
 * <p>
 * The events of a namespace are keyed by their path and sorted by it. Every event is encoded with the
 * {@link SoundEventDefinitionCodec}, so an invalid sound aborts the generation with a message that names the sound.
 * The generation runs read only inside a transaction, because the sound data of an event is loaded lazily.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 0.3.2
 */
@Singleton
public class SoundJsonGenerator extends FileGenerator {

    private static final String ASSETS_FOLDER = "assets";

    private final SoundRepository soundRepository;

    @Inject
    public SoundJsonGenerator(@NotNull SoundRepository soundRepository) {
        super("sounds.json");
        this.soundRepository = soundRepository;
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path javaPath, @NotNull UUID projectId) {
        List<SoundEventEntity> entities = this.soundRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
        if (entities.isEmpty()) return;

        Map<String, Map<String, JsonElement>> namespaces = new TreeMap<>();
        for (SoundEventEntity entity : entities) {
            SoundEventKey key = SoundEventKey.of(entity);
            Map<String, JsonElement> events = namespaces.computeIfAbsent(key.namespace(), namespace -> new TreeMap<>());
            if (events.containsKey(key.path())) {
                throw new IllegalArgumentException("sound event '" + key.asString() + "' is defined more than once");
            }
            events.put(key.path(), encode(key, entity));
        }

        namespaces.forEach((namespace, events) -> write(javaPath, namespace, events));
    }

    /**
     * Encodes one sound event with the codec.
     *
     * @throws IllegalArgumentException if the event is invalid, the message names the sound and the reason
     */
    private static JsonElement encode(SoundEventKey key, SoundEventEntity entity) {
        SoundEventDefinition definition = SoundEventDefinition.from(entity);
        Result<JsonElement> result = SoundEventDefinitionCodec.CODEC.encode(Transcoder.JSON, definition);
        if (result instanceof Result.Ok<JsonElement> ok) {
            return ok.value();
        }
        String reason = result instanceof Result.Error<JsonElement> error ? error.message() : "no value encoded";
        throw new IllegalArgumentException("sound '" + key.asString() + "' is invalid: " + reason);
    }

    private void write(Path javaPath, String namespace, Map<String, JsonElement> events) {
        JsonObject root = new JsonObject();
        events.forEach(root::add);

        Path directory = javaPath.resolve(ASSETS_FOLDER).resolve(namespace);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        save(directory.resolve(fileName), GsonHolder.GSON, root);
    }

    @Override
    public @NotNull String getName() {
        return SoundJsonGenerator.class.getSimpleName();
    }
}
