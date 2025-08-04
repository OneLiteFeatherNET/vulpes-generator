package net.theevilreaper.vulpes.generator.registry;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.Generator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.jetbrains.annotations.ApiStatus.*;

/**
 * The {@link VulpesGeneratorRegistry} is the default registry implementation of the {@link GeneratorRegistry}
 * to manage different implementation of a {@link AbstractCodeGenerator}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 0.1.0
 */
@Internal
@NonExtendable
@Singleton
final class VulpesGeneratorRegistry implements GeneratorRegistry {

    private final Map<Class<? extends Generator>, Provider<Generator>> generators;

    /**
     * Creates a new instance of the registry.
     */
    private VulpesGeneratorRegistry() {
        this.generators = new ConcurrentHashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(@NotNull Generator generator) {
        generators.put(generator.getClass(), () -> generator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable <T extends Generator> Generator remove(@NotNull Class<T> generatorClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @Nullable <T extends Generator> Generator get(@NotNull Class<T> generatorClass) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void triggerAll(@NotNull Path path) {
        this.generators.values().forEach(generator -> generator.get().generate(path));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Contract(pure = true)
    public @NotNull @UnmodifiableView Map<Class<? extends Generator>, Generator> getAll() {
        return generators.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().get()
                ));
    }
}
