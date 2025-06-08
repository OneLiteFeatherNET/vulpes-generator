package net.theevilreaper.vulpes.generator.registry;

import net.onelitefeather.vulpes.api.model.VulpesModel;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.file.Path;
import java.util.Map;

/**
 * Defines the contract for a registry managing {@link AbstractCodeGenerator} instances.
 */
public sealed interface GeneratorRegistry permits VulpesGeneratorRegistry {

    /**
     * Adds a new generator to the registry.
     *
     * @param generator The generator to register.
     * @return The previous generator associated with the name, or {@code null} if none existed.
     */
    AbstractCodeGenerator<? extends VulpesModel> add(@NotNull AbstractCodeGenerator<? extends VulpesModel> generator);

    /**
     * Removes a generator from the registry by name.
     *
     * @param name The name of the generator to remove.
     * @return The removed generator, or {@code null} if it was not registered.
     */
    AbstractCodeGenerator<? extends VulpesModel> remove(@NotNull String name);

    /**
     * Retrieves a generator by its name.
     *
     * @param name The name of the generator.
     * @return The generator, or {@code null} if not found.
     */
    AbstractCodeGenerator<? extends VulpesModel> get(@NotNull String name);

    /**
     * Executes all registered generators with the given path.
     *
     * @param path The target path where generation should occur.
     */
    void triggerAll(@NotNull Path path);

    /**
     * Returns an unmodifiable view of the registered generators.
     *
     * @return An unmodifiable map of generator names to instances.
     */
    @NotNull @UnmodifiableView Map<String, AbstractCodeGenerator<? extends VulpesModel>> getAll();
}
