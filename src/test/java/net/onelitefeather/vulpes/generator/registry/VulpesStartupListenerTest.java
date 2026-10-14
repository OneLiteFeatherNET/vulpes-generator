package net.onelitefeather.vulpes.generator.registry;

import io.micronaut.context.event.StartupEvent;
import net.onelitefeather.vulpes.generator.generation.Generator;
import net.onelitefeather.vulpes.generator.generation.java.AdvancementGenerator;
import net.onelitefeather.vulpes.generator.generation.java.AttributeGenerator;
import net.onelitefeather.vulpes.generator.generation.java.FontGenerator;
import net.onelitefeather.vulpes.generator.generation.java.ItemGenerator;
import net.onelitefeather.vulpes.generator.generation.java.SoundGenerator;
import net.onelitefeather.vulpes.generator.generation.json.FontJsonGenerator;
import net.onelitefeather.vulpes.generator.generation.json.ItemJsonGenerator;
import net.onelitefeather.vulpes.generator.generation.json.SoundJsonGenerator;
import net.onelitefeather.vulpes.generator.listener.VulpesStartupListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class VulpesStartupListenerTest {

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path tempDir;

    private VulpesGeneratorRegistry registry;
    private VulpesStartupListener listener;
    private List<Generator> generators;

    @BeforeEach
    void setUp() {
        registry = new VulpesGeneratorRegistry();
        AttributeGenerator attributeGenerator = mock(AttributeGenerator.class);
        FontGenerator fontGenerator = mock(FontGenerator.class);
        ItemGenerator itemGenerator = mock(ItemGenerator.class);
        AdvancementGenerator advancementGenerator = mock(AdvancementGenerator.class);
        SoundGenerator soundGenerator = mock(SoundGenerator.class);
        SoundJsonGenerator soundJsonGenerator = mock(SoundJsonGenerator.class);
        ItemJsonGenerator itemJsonGenerator = mock(ItemJsonGenerator.class);
        FontJsonGenerator fontJsonGenerator = mock(FontJsonGenerator.class);

        generators = List.of(attributeGenerator, fontGenerator, itemGenerator, advancementGenerator, soundGenerator,
                soundJsonGenerator, itemJsonGenerator, fontJsonGenerator);

        listener = new VulpesStartupListener(registry,
                attributeGenerator, fontGenerator, itemGenerator, advancementGenerator, soundGenerator,
                soundJsonGenerator, itemJsonGenerator, fontJsonGenerator);
    }

    @Test
    void registersEveryJavaAndJsonGeneratorOnStartup() {
        listener.onApplicationEvent(mock(StartupEvent.class));

        Set<Class<? extends Generator>> expected = Set.of(
                AttributeGenerator.class, FontGenerator.class, ItemGenerator.class, AdvancementGenerator.class,
                SoundGenerator.class, SoundJsonGenerator.class, ItemJsonGenerator.class, FontJsonGenerator.class);

        assertEquals(expected, registry.getAll().keySet(),
                "Every Java and JSON generator must be registered at startup");
    }

    @Test
    void triggerAllCallsEachRegisteredGeneratorExactlyOnce() {
        listener.onApplicationEvent(mock(StartupEvent.class));

        registry.triggerAll(tempDir, PROJECT_ID);

        for (Generator generator : generators) {
            verify(generator, times(1)).generate(tempDir, PROJECT_ID);
        }
    }
}
