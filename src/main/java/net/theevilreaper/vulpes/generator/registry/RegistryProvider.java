package net.theevilreaper.vulpes.generator.registry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator;
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator;
import net.theevilreaper.vulpes.generator.generation.java.ItemGenerator;
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Provides a pre-configured {@link GeneratorRegistry} instance
 * with all available generators registered.
 */
@Singleton
public final class RegistryProvider {

    private final GeneratorRegistry generatorRegistry;

    /**
     * Creates a new instance of the {@link RegistryProvider}.
     * <p>
     * Automatically registers all provided generators into the {@link GeneratorRegistry}.
     * </p>
     *
     * @param attributeGenerator    The attribute generator.
     * @param fontGenerator         The font generator.
     * @param itemGenerator         The item generator.
     * @param notificationGenerator The notification generator.
     */
    @Inject
    RegistryProvider(
            @NotNull AttributeGenerator attributeGenerator,
            @NotNull FontGenerator fontGenerator,
            @NotNull ItemGenerator itemGenerator,
            @NotNull NotificationGenerator notificationGenerator
    ) {
        this.generatorRegistry = new VulpesGeneratorRegistry();
        this.generatorRegistry.add(attributeGenerator);
        this.generatorRegistry.add(fontGenerator);
        this.generatorRegistry.add(itemGenerator);
        this.generatorRegistry.add(notificationGenerator);
    }

    /**
     * Returns the generator registry containing all registered generators.
     *
     * @return The {@link GeneratorRegistry} instance.
     */
    public @NotNull GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }
}
