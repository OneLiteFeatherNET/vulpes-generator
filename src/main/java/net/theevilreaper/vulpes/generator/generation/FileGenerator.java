package net.theevilreaper.vulpes.generator.generation;

import org.jetbrains.annotations.NotNull;

public abstract class FileGenerator implements Generator {

    protected final String fileName;

    protected FileGenerator(@NotNull String fileName) {
        this.fileName = fileName;
    }


    @Override
    public void cleanup() {
        throw new UnsupportedOperationException("Cleanup is not supported for this generator");
    }
}
