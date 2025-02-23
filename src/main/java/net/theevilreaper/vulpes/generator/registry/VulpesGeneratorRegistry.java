package net.theevilreaper.vulpes.generator.registry;

import net.theevilreaper.vulpes.api.model.VulpesModel;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link GeneratorRegistry} using an in-memory {@link ConcurrentHashMap}.
 */
@ApiStatus.Internal
@ApiStatus.NonExtendable
final class VulpesGeneratorRegistry implements GeneratorRegistry {

    private final Map<String, AbstractCodeGenerator<? extends VulpesModel>> generators;

    /**
     * Creates an empty generator registry.
     */
    VulpesGeneratorRegistry() {
        this.generators = new ConcurrentHashMap<>();
    }

    @Override
    public AbstractCodeGenerator<? extends VulpesModel> add(@NotNull AbstractCodeGenerator<? extends VulpesModel> generator) {
        return this.generators.put(generator.getName(), generator);
    }

    @Override
    public AbstractCodeGenerator<? extends VulpesModel> remove(@NotNull String name) {
        return this.generators.remove(name);
    }

    @Override
    public AbstractCodeGenerator<? extends VulpesModel> get(@NotNull String name) {
        return this.generators.get(name);
    }

    @Override
    public void triggerAll(@NotNull Path path) {
        this.generators.values().forEach(generator -> generator.generate(path));
    }

    @Override
    @Contract(pure = true)
    public @NotNull @UnmodifiableView Map<String, AbstractCodeGenerator<? extends VulpesModel>> getAll() {
        return Collections.unmodifiableMap(generators);
    }
}
