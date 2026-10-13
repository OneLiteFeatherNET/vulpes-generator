package net.onelitefeather.vulpes.generator.generation.java;

import net.onelitefeather.vulpes.generator.generation.JavaIdentifiers;
import com.google.gson.JsonElement;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.advancements.Advancement;
import net.minestom.server.advancements.AdvancementManager;
import net.minestom.server.advancements.AdvancementRoot;
import net.minestom.server.advancements.AdvancementTab;
import net.minestom.server.advancements.FrameType;
import net.minestom.server.item.Material;
import net.onelitefeather.vulpes.api.model.advancement.AdvancementEntity;
import net.onelitefeather.vulpes.api.repository.AdvancementRepository;
import net.onelitefeather.vulpes.generator.generation.AbstractCodeGenerator;
import net.onelitefeather.vulpes.generator.generation.JavaStructure;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementData;
import net.onelitefeather.vulpes.generator.generation.component.AdvancementTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static net.onelitefeather.vulpes.generator.util.Constants.INDENT_DEFAULT;
import static net.onelitefeather.vulpes.generator.util.Constants.JAVA_MAIM_FOLDER;

/**
 * Generates the {@code AdvancementRegistry} class of a project.
 * <p>
 * The class holds one {@code public static final String} per advancement with its namespaced key. The static method
 * {@code register(AdvancementManager)} creates one {@link AdvancementTab} per root and attaches the children in tree
 * order, so a parent always exists before its children. It returns the tabs by the namespaced key of their root.
 * <p>
 * Titles and descriptions are emitted as JSON text and deserialized at runtime. All values are validated by
 * {@link AdvancementTree} at generation time, so invalid data aborts the generation instead of ending up in the code.
 * The generation runs read only inside a transaction, because the parent relations are loaded lazily.
 */
@Prototype
public class AdvancementGenerator extends AbstractCodeGenerator<AdvancementEntity> implements JavaStructure {

    private static final String REGISTER = "register";
    private static final String MANAGER = "manager";
    private static final String TABS = "tabs";
    private static final String ADVANCEMENT_PREFIX = "adv_";
    private static final String TAB_PREFIX = "tab_";

    private static final ClassName ADVANCEMENT = ClassName.get(Advancement.class);
    private static final ClassName ADVANCEMENT_ROOT = ClassName.get(AdvancementRoot.class);
    private static final ClassName ADVANCEMENT_TAB = ClassName.get(AdvancementTab.class);
    private static final ClassName ADVANCEMENT_MANAGER = ClassName.get(AdvancementManager.class);
    private static final ClassName FRAME_TYPE = ClassName.get(FrameType.class);
    private static final ClassName MATERIAL = ClassName.get(Material.class);
    private static final ClassName COMPONENT = ClassName.get(Component.class);
    private static final ClassName GSON_COMPONENT_SERIALIZER = ClassName.get(GsonComponentSerializer.class);
    private static final ClassName STRING = ClassName.get(String.class);
    private static final ClassName MAP = ClassName.get(Map.class);
    private static final ClassName LINKED_HASH_MAP = ClassName.get(LinkedHashMap.class);
    private static final ClassName COLLECTIONS = ClassName.get(Collections.class);

    private final AdvancementRepository advancementRepository;

    @Inject
    public AdvancementGenerator(@NotNull AdvancementRepository advancementRepository) {
        super("AdvancementRegistry", BASE_PACKAGE + ".advancement");
        this.advancementRepository = advancementRepository;
    }

    @Override
    protected List<AdvancementEntity> getModels(@NotNull UUID projectId) {
        return this.advancementRepository.findByProjectId(projectId, Pageable.UNPAGED).getContent();
    }

    @Override
    @ReadOnly
    public void generate(@NotNull Path root, @NotNull UUID projectId) {
        cleanup();
        List<AdvancementEntity> models = getModels(projectId);

        if (models.isEmpty()) return;

        List<AdvancementData> advancements = AdvancementTree.from(models);

        addClassModifiers(this.classBuilder);
        addJetbrainsAnnotation(this.classBuilder);
        addPrivateDefaultConstructor(this.classBuilder);
        addDefaultSuppressAnnotation(this.classBuilder);

        this.classBuilder.addFields(createFields(advancements));
        this.classBuilder.addMethod(createRegisterMethod(advancements));

        filesToGenerate.clear();
        filesToGenerate.add(
                JavaFile.builder(packageName, this.classBuilder.build())
                        .indent(INDENT_DEFAULT)
                        .skipJavaLangImports(true)
                        .build()
        );
        writeFiles(root.resolve(JAVA_MAIM_FOLDER));
    }

    @Override
    public @NotNull String getName() {
        return "AdvancementGenerator";
    }

    /**
     * Creates one constant per advancement, sorted by variable name.
     */
    private static List<FieldSpec> createFields(List<AdvancementData> advancements) {
        JavaIdentifiers.Claims fieldNames = new JavaIdentifiers.Claims();
        return advancements.stream()
                .sorted(Comparator.comparing(AdvancementData::variableName))
                .map(advancement -> FieldSpec.builder(STRING, fieldNames.claim(JavaIdentifiers.constantName(advancement.variableName()), advancement.key()))
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$S", advancement.namespacedKey())
                        .build())
                .toList();
    }

    /**
     * Creates {@code public static Map<String, AdvancementTab> register(AdvancementManager manager)}. The advancements
     * are already in tree order, so each parent is created before its children are attached.
     */
    private static MethodSpec createRegisterMethod(List<AdvancementData> advancements) {
        Map<String, AdvancementData> byNamespacedKey = new HashMap<>();
        advancements.forEach(advancement -> byNamespacedKey.put(advancement.namespacedKey(), advancement));

        MethodSpec.Builder method = MethodSpec.methodBuilder(REGISTER)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ParameterizedTypeName.get(MAP, STRING, ADVANCEMENT_TAB))
                .addParameter(ADVANCEMENT_MANAGER, MANAGER)
                .addStatement("$T<$T, $T> $L = new $T<>()", MAP, STRING, ADVANCEMENT_TAB, TABS, LINKED_HASH_MAP);

        JavaIdentifiers.Claims localNames = new JavaIdentifiers.Claims();
        for (AdvancementData advancement : advancements) {
            String local = localNames.claim(JavaIdentifiers.localName(ADVANCEMENT_PREFIX, advancement.variableName()), advancement.key());
            if (advancement.isRoot()) {
                addRoot(method, advancement, local, localNames);
            } else {
                AdvancementData parent = byNamespacedKey.get(advancement.parentNamespacedKey());
                String tab = tabName(byNamespacedKey.get(advancement.rootNamespacedKey()));
                addChild(method, advancement, local, JavaIdentifiers.localName(ADVANCEMENT_PREFIX, parent.variableName()), tab);
            }
        }

        method.addStatement("return $T.unmodifiableMap($L)", COLLECTIONS, TABS);
        return method.build();
    }

    /**
     * Creates the root, its tab and registers the tab in the result map.
     */
    private static void addRoot(MethodSpec.Builder method, AdvancementData root, String local, JavaIdentifiers.Claims localNames) {
        String tab = localNames.claim(tabName(root), root.key());
        method.addStatement("$T $L = new $T($L, $L, $T.$L, $T.$L, $L, $L, $L)",
                ADVANCEMENT_ROOT, local, ADVANCEMENT_ROOT,
                textComponent(root.title()), textComponent(root.description()),
                MATERIAL, root.material().key().value().toUpperCase(Locale.ROOT),
                FRAME_TYPE, root.frameType().name(),
                floatLiteral(root.x()), floatLiteral(root.y()),
                backgroundLiteral(root.background()));
        addFlags(method, root, local);
        method.addStatement("$T $L = $L.createTab($S, $L)", ADVANCEMENT_TAB, tab, MANAGER, root.namespacedKey(), local);
        method.addStatement("$L.put($S, $L)", TABS, root.namespacedKey(), tab);
    }

    /**
     * Creates a child and attaches it to the tab of its root with its parent.
     */
    private static void addChild(MethodSpec.Builder method, AdvancementData child, String local, String parentLocal, String tab) {
        method.addStatement("$T $L = new $T($L, $L, $T.$L, $T.$L, $L, $L)",
                ADVANCEMENT, local, ADVANCEMENT,
                textComponent(child.title()), textComponent(child.description()),
                MATERIAL, child.material().key().value().toUpperCase(Locale.ROOT),
                FRAME_TYPE, child.frameType().name(),
                floatLiteral(child.x()), floatLiteral(child.y()));
        addFlags(method, child, local);
        method.addStatement("$L.createAdvancement($S, $L, $L)", tab, child.namespacedKey(), local, parentLocal);
    }

    /**
     * Applies the toast and visibility flags. Minestom has no setter for the chat announcement, so
     * {@link AdvancementData#announceToChat()} is validated but not emitted.
     */
    private static void addFlags(MethodSpec.Builder method, AdvancementData advancement, String local) {
        method.addStatement("$L.showToast($L)", local, advancement.showToast());
        method.addStatement("$L.setHidden($L)", local, advancement.hidden());
    }

    /**
     * Emits the title or description as {@code GsonComponentSerializer.gson().deserialize(json)}, or {@code Component.empty()}
     * if the value is missing.
     */
    private static CodeBlock textComponent(@Nullable JsonElement text) {
        if (text == null) {
            return CodeBlock.of("$T.empty()", COMPONENT);
        }
        return CodeBlock.of("$T.gson().deserialize($S)", GSON_COMPONENT_SERIALIZER, text.toString());
    }

    private static CodeBlock backgroundLiteral(@Nullable String background) {
        return background == null ? CodeBlock.of("null") : CodeBlock.of("$S", background);
    }

    private static String floatLiteral(float value) {
        return Float.toString(value) + "f";
    }

    private static String tabName(AdvancementData root) {
        return JavaIdentifiers.localName(TAB_PREFIX, root.variableName());
    }
}
