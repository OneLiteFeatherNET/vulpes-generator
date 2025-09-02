package net.theevilreaper.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.NotificationEntity;
import net.onelitefeather.vulpes.api.repository.NotificationRepository;
import net.theevilreaper.vulpes.generator.generation.FileGenerator;
import net.theevilreaper.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Singleton
public class NotificationJsonGenerator extends FileGenerator {

    private final NotificationRepository notificationRepository;

    @Inject
    public NotificationJsonGenerator(@NotNull NotificationRepository notificationRepository) {
        super("notifications.json");
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        Path filePath = javaPath.resolve(fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<NotificationEntity> entities = this.notificationRepository.findAll();

        if (entities.isEmpty()) return;

        JsonArray elements = new JsonArray();

        for (NotificationEntity entity : entities) {
            JsonObject object = new JsonObject();
            object.addProperty("variableName", entity.getVariableName());
            object.addProperty("material", entity.getMaterial());
            object.addProperty("title", entity.getTitle());
            object.addProperty("frameType", entity.getFrameType());

            elements.add(object);
        }

        save(filePath, GsonHolder.GSON, elements);
    }

    @Override
    public @NotNull String getName() {
        return NotificationJsonGenerator.class.getSimpleName();
    }
}
