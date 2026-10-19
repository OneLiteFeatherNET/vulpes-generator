package net.onelitefeather.vulpes.generator.listener;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import net.onelitefeather.vulpes.generator.generation.java.AdvancementGenerator;
import net.onelitefeather.vulpes.generator.generation.java.AttributeGenerator;
import net.onelitefeather.vulpes.generator.generation.java.FontGenerator;
import net.onelitefeather.vulpes.generator.generation.java.ItemGenerator;
import net.onelitefeather.vulpes.generator.generation.java.SoundGenerator;
import net.onelitefeather.vulpes.generator.generation.json.FontJsonGenerator;
import net.onelitefeather.vulpes.generator.generation.json.SoundJsonGenerator;
import net.onelitefeather.vulpes.generator.registry.GeneratorRegistry;

@Singleton
public class VulpesStartupListener implements ApplicationEventListener<StartupEvent> {

    private final GeneratorRegistry generatorRegistry;
    private final AttributeGenerator attributeGenerator;
    private final FontGenerator fontGenerator;
    private final ItemGenerator itemGenerator;
    private final AdvancementGenerator advancementGenerator;
    private final SoundGenerator soundGenerator;
    private final SoundJsonGenerator soundJsonGenerator;
    private final FontJsonGenerator fontJsonGenerator;

    @Inject
    public VulpesStartupListener(
            GeneratorRegistry generatorRegistry,
            AttributeGenerator attributeGenerator,
            FontGenerator fontGenerator,
            ItemGenerator itemGenerator,
            AdvancementGenerator advancementGenerator,
            SoundGenerator soundGenerator,
            SoundJsonGenerator soundJsonGenerator,
            FontJsonGenerator fontJsonGenerator
    ) {
        this.generatorRegistry = generatorRegistry;
        this.attributeGenerator = attributeGenerator;
        this.fontGenerator = fontGenerator;
        this.itemGenerator = itemGenerator;
        this.advancementGenerator = advancementGenerator;
        this.soundGenerator = soundGenerator;
        this.soundJsonGenerator = soundJsonGenerator;
        this.fontJsonGenerator = fontJsonGenerator;
    }

    @Override
    public void onApplicationEvent(StartupEvent event) {
        this.generatorRegistry.add(attributeGenerator);
        this.generatorRegistry.add(fontGenerator);
        this.generatorRegistry.add(itemGenerator);
        this.generatorRegistry.add(advancementGenerator);
        this.generatorRegistry.add(soundGenerator);
        this.generatorRegistry.add(soundJsonGenerator);
        this.generatorRegistry.add(fontJsonGenerator);
    }
}
