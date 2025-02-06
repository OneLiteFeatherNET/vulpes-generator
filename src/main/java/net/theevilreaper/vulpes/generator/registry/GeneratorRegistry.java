package net.theevilreaper.vulpes.generator.registry;

import net.theevilreaper.vulpes.generator.generation.AbstractCodeGenerator;
import net.theevilreaper.vulpes.generator.generation.Generator;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class GeneratorRegistry {

    private final Map<String, AbstractCodeGenerator<?>> generators;

    public GeneratorRegistry() {
        this.generators = new HashMap<>();
    }

    public void add(AbstractCodeGenerator<?> generator) {
        this.generators.put(generator.getName(), generator);
    }

    public void remove(String name) {
        this.generators.remove(name);
    }

    public void triggerAll(@NotNull Path path) {
        this.generators.values().forEach(generator -> generator.generate(path));
    }
}
