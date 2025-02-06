package net.theevilreaper.vulpes.generator.registry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator;
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator;
import net.theevilreaper.vulpes.generator.generation.java.ItemGenerator;
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator;
import org.jetbrains.annotations.NotNull;

@Singleton
public final class RegistryProvider {

    private final GeneratorRegistry generatorRegistry;

    @Inject
    public RegistryProvider(
            @NotNull AttributeGenerator attributeGenerator,
            @NotNull FontGenerator fontGenerator,
            @NotNull ItemGenerator itemGenerator,
            @NotNull NotificationGenerator notificationGenerator
    ) {
        this.generatorRegistry = new GeneratorRegistry();
        this.generatorRegistry.add(attributeGenerator);
        this.generatorRegistry.add(fontGenerator);
        this.generatorRegistry.add(itemGenerator);
        this.generatorRegistry.add(notificationGenerator);
    }

    public GeneratorRegistry getGeneratorRegistry() {
        return generatorRegistry;
    }
}
