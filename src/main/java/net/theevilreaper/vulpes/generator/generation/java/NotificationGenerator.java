package net.theevilreaper.vulpes.generator.generation.java;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.kyori.adventure.text.Component;
import net.minestom.server.advancements.Advancement;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.NotificationEntity;
import net.onelitefeather.vulpes.api.repository.NotificationRepository;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.JavaStructure;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.theevilreaper.vulpes.generator.util.Constants.INDENT_DEFAULT;

@Singleton
public class NotificationGenerator extends AbstractCodeGenerator<NotificationEntity> implements JavaStructure {

    private final NotificationRepository notificationRepository;

    @Inject
    public NotificationGenerator(@NotNull NotificationRepository notificationRepository) {
        super("Notification", BASE_PACKAGE + ".notification");
        this.notificationRepository = notificationRepository;
    }

    @Override
    protected List<NotificationEntity> getModels() {
        return this.notificationRepository.findAll();
    }

    @Override
    public void generate(@NotNull Path javaPath) {
        var models = getModels();
        if (models.isEmpty()) return;

        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);
        var fields = getFields(models).values();
        this.classBuilder.addFields(fields);
        JavaFile javaFile = JavaFile.builder(packageName, this.classBuilder.build())
                .indent(INDENT_DEFAULT)
                .skipJavaLangImports(true)
                .build();
        writeFiles(List.of(javaFile), javaPath);
    }

    private Map<String, FieldSpec> getFields(List<NotificationEntity> models) {
        Map<String, FieldSpec> fields = new HashMap<>();
        ClassName className = ClassName.get(Advancement.class);
        models.stream().filter(model -> model.getName() == null || !model.getModelName().isEmpty())
                .forEach(model -> {
                    if (fields.containsKey(model.getName())) return;
                    FrameType frameType = FrameType.valueOf(model.getFrameType().toUpperCase());
                    String title = (model.getTitle() == null || model.getTitle().isEmpty()) ? EMPTY_COMPONENT : getTextContent(model.getTitle());
                    String description = (model.getDescription() == null || model.getDescription().isEmpty()) ? EMPTY_COMPONENT : getTextContent(model.getDescription());
                    Material material = (model.getMaterial() == null || model.getMaterial().isEmpty()) ? Material.STONE : Material.fromKey(model.getMaterial());
                    FieldSpec field = FieldSpec.builder(
                            className, model.getName().toUpperCase()
                    )
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer(
                                    CodeBlock.of(
                                            "new \\$T(\\$T.\\$L, Component.\\$L, Material.\\$L, \\$T.\\$L, \\$L, \\$L)",
                                            className,
                                            ClassName.get(Component.class),
                                            title,
                                            description,
                                            material,
                                            ClassName.get(FrameType.class),
                                            frameType,
                                            0,
                                            0
                                    )
                            )
                            .build();
                    fields.put(model.getName(), field);
                });

        return fields;
    }

    @Override
    public @NotNull String getName() {
        return "NotificationGenerator";
    }

    private @NotNull String getTextContent(String input) {
        return "text(" + input + ")";
    }
}
