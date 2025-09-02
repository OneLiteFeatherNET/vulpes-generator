package net.theevilreaper.vulpes.generator.generation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class FileGenerator implements Generator {

    protected final String fileName;

    protected FileGenerator(@NotNull String fileName) {
        this.fileName = fileName;
    }

    /**
     * Saves the given object to the given path.
     *
     * @param path   the path to save the object to
     * @param gson   the gson instance to use for serialization
     * @param object the object to save
     * @param <T>    the type of the object to save
     */
    protected <T> void save(@NotNull Path path, @NotNull Gson gson, @NotNull T object) {
        Check.argCondition(Files.isDirectory(path), "Unable to save a directory. Please check the used path");
        try (var outputStream = Files.newBufferedWriter(path, UTF_8)) {
            if (!Files.exists(path)) {
                Files.createFile(path);
            }
            gson.toJson(object, TypeToken.get(object.getClass()).getType(), outputStream);
        } catch (IOException exception) {
            //TODO: Better logging
            System.out.println(exception.getMessage());
        }
    }

    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Cleanup is not supported for this generator");
    }
}
