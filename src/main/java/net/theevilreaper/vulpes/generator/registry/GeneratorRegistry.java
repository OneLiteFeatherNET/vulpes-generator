package net.theevilreaper.vulpes.generator.registry;

import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.Generator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.file.Path;
import java.util.Map;

/**
 * Defines the contract for a registry managing {@link AbstractCodeGenerator} instances.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 0.1.0
 */
public sealed interface GeneratorRegistry permits VulpesGeneratorRegistry {

    /**
     * Adds a new generator to the registry.
     *
     * @param generator The generator to register.
     */
    void add(@NotNull Generator generator);

    /**
     * Removes a generator from the registry by its name.
     *
     * @param generatorClass the class of the generator to remove.
     * @param <T>            the type of the generator.
     * @return the removed generator instance, or {@code null} if not found.
     */
    @Nullable <T extends Generator> Generator remove(@NotNull Class<T> generatorClass);

    /**
     * Retrieves a generator by its class type.
     *
     * @param generatorClass The class of the generator to retrieve.
     * @param <T>            The type of the generator.
     * @return The generator instance, or {@code null} if not found.
     */
    @Nullable <T extends Generator> Generator get(@NotNull Class<T> generatorClass);

    /**
     * Executes all registered generators with the given path.
     *
     * @param path the target path where generation should occur.
     */
    void triggerAll(@NotNull Path path);

    /**
     * Returns an unmodifiable view of the registered generators.
     *
     * @return an unmodifiable view
     */
    @NotNull
    @UnmodifiableView
    Map<Class<? extends Generator>, Generator> getAll();
}
