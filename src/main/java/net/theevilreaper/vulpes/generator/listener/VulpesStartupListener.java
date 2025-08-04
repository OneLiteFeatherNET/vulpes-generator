package net.theevilreaper.vulpes.generator.listener;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.theevilreaper.vulpes.generator.generation.java.AttributeGenerator;
import net.theevilreaper.vulpes.generator.generation.java.FontGenerator;
import net.theevilreaper.vulpes.generator.generation.java.ItemGenerator;
import net.theevilreaper.vulpes.generator.generation.java.NotificationGenerator;
import net.theevilreaper.vulpes.generator.registry.GeneratorRegistry;

@Singleton
public class VulpesStartupListener implements ApplicationEventListener<StartupEvent> {

    private final GeneratorRegistry generatorRegistry;
    private final AttributeGenerator attributeGenerator;
    private final FontGenerator fontGenerator;
    private final ItemGenerator itemGenerator;
    private final NotificationGenerator notificationGenerator;

    @Inject
    public VulpesStartupListener(
            GeneratorRegistry generatorRegistry,
            AttributeGenerator attributeGenerator,
            FontGenerator fontGenerator,
            ItemGenerator itemGenerator,
            NotificationGenerator notificationGenerator
    ) {
        this.generatorRegistry = generatorRegistry;
        this.attributeGenerator = attributeGenerator;
        this.fontGenerator = fontGenerator;
        this.itemGenerator = itemGenerator;
        this.notificationGenerator = notificationGenerator;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        this.generatorRegistry.add(attributeGenerator);
        this.generatorRegistry.add(fontGenerator);
        this.generatorRegistry.add(itemGenerator);
        this.generatorRegistry.add(notificationGenerator);
    }
}
