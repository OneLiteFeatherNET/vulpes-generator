package net.theevilreaper.vulpes.generator.generation;

import com.squareup.javapoet.JavaFile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@link AbstractGenerator<T>} class is an abstract base implementation for the {@link Generator} interface.
 * It provides the implementation for some common methods and fields in a generation context.
 * This class is just a generic implementation and must be extended by a concrete generator class.
 *
 * @param <T> the type of the model to be generated
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class AbstractGenerator<T> implements Generator {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractGenerator.class);
    private static final String EMPTY_FILES_TO_WRITE = "No files to write. Skipping the file write";

    protected final List<JavaFile> filesToGenerate;

    protected AbstractGenerator() {
        this.filesToGenerate = new ArrayList<>();
    }

    /**
     * Clears some fields and prepares the generator for the next generation.
     */
    @Override
    public void cleanup() {
        this.filesToGenerate.clear();
    }

    /**
     * Writes a list of files to the given output folder.
     *
     * @param fileList     list of files to write
     * @param outputFolder the output folder
     */
    protected void writeFiles(@NotNull List<JavaFile> fileList, @NotNull Path outputFolder) {
        if (checkIfEmpty(filesToGenerate)) return;
        write(fileList, outputFolder);
    }

    /**
     * Writes the files to the given output folder.
     *
     * @param outputFolder the output folder
     */
    protected void writeFiles(@NotNull Path outputFolder) {
        if (checkIfEmpty(filesToGenerate)) return;
        write(filesToGenerate, outputFolder);
    }

    /**
     * Writes the given files to the output folder.
     *
     * @param files        the files to write
     * @param outputFolder the output folder
     */
    private void write(@NotNull List<JavaFile> files, @NotNull Path outputFolder) {
        files.forEach(file -> {
            try {
                file.writeTo(outputFolder);
            } catch (Exception e) {
                LOGGER.error("Failed to write file: {}", e.getMessage());
            }
        });
    }

    /**
     * Checks if the list of files is empty.
     * @param files the list of files to check
     * @return true if the list is empty, false otherwise
     */
    protected boolean checkIfEmpty(@NotNull List<JavaFile> files) {
        if (files.isEmpty()) {
            LOGGER.info(EMPTY_FILES_TO_WRITE);
            return true;
        }
        return false;
    }

    /**
     * Returns the list of models to be generated.
     *
     * @return the list of models
     */
    protected abstract @NotNull List<T> getModels();

}
