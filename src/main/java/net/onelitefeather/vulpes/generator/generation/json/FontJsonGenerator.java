package net.onelitefeather.vulpes.generator.generation.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.api.model.FontEntity;
import net.onelitefeather.vulpes.api.repository.FontRepository;
import net.onelitefeather.vulpes.generator.generation.FileGenerator;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec;
import net.onelitefeather.vulpes.generator.generation.font.BitmapFontProviderCodec.BitmapFontProvider;
import net.onelitefeather.vulpes.generator.gson.GsonHolder;
import org.jetbrains.annotations.NotNull;

import static net.onelitefeather.vulpes.generator.util.Constants.RESOURCES_FOLDER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the resource pack font files {@code assets/<namespace>/font/<mapper>.json}.
 * <p>
 * Every font becomes a bitmap provider of the file named by its mapper. Fonts with the same mapper share one file, their
 * providers are sorted by key. The namespace is the project part of the namespaced key of the fonts.
 * The files are written below the given output directory.
 * <p>
 * The generation runs read only inside a transaction, because the chars of a font are loaded lazily.
 */
@Singleton
public class FontJsonGenerator extends FileGenerator {

    private static final String ASSETS_DIRECTORY = "assets";
    private static final String FONT_DIRECTORY = "font";
    private static final String JSON_EXTENSION = ".json";

    private final FontRepository fontRepository;

    @Inject
    public FontJsonGenerator(@NotNull FontRepository fontRepository) {
        super("font");
        this.fontRepository = fontRepository;
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        List<FontEntity> fonts = this.fontRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
        if (fonts.isEmpty()) return;

        TreeMap<String, List<FontEntity>> fontsByMapper = new TreeMap<>();
        for (FontEntity font : fonts) {
            String mapper = font.getMapper();
            if (mapper == null || mapper.isBlank()) {
                throw new IllegalArgumentException("font '" + font.getKey() + "' has no mapper");
            }
            fontsByMapper.computeIfAbsent(mapper, _ -> new ArrayList<>()).add(font);
        }

        for (var entry : fontsByMapper.entrySet()) {
            List<FontEntity> group = entry.getValue().stream()
                    .sorted(Comparator.comparing(FontEntity::getKey))
                    .toList();
            writeFontFile(root, BitmapFontProviderCodec.namespaceOf(group.getFirst()), entry.getKey(), group);
        }
    }

    private void writeFontFile(@NotNull Path root, @NotNull String namespace, @NotNull String mapper, @NotNull List<FontEntity> group) {
        JsonArray providers = new JsonArray();
        for (FontEntity font : group) {
            BitmapFontProvider provider = BitmapFontProviderCodec.fromFont(font);
            providers.add(BitmapFontProviderCodec.toJson(provider));
        }

        JsonObject document = new JsonObject();
        document.add("providers", providers);

        Path directory = root.resolve(RESOURCES_FOLDER).resolve(ASSETS_DIRECTORY).resolve(namespace).resolve(FONT_DIRECTORY);
        Path file = directory.resolve(mapper + JSON_EXTENSION);
        try {
            Files.createDirectories(directory);
            Files.writeString(file, GsonHolder.GSON.toJson(document), UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("unable to write font file " + file, e);
        }
    }

    @Override
    public @NotNull String getName() {
        return FontJsonGenerator.class.getSimpleName();
    }
}
