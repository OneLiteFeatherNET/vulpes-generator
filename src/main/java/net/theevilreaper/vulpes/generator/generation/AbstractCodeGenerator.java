package net.theevilreaper.vulpes.generator.generation;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import net.onelitefeather.vulpes.api.model.VulpesModel;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCodeGenerator<T extends VulpesModel> implements Generator {

    private static final String EMPTY_FILES_TO_WRITE = "No files to write. Skipping the file write";

    protected static final String BASE_PACKAGE = "net.reaper.vulpes";
    protected static final String EMPTY_COMPONENT = "empty()";

    protected Logger logger = LoggerFactory.getLogger(AbstractCodeGenerator.class);

    protected final String className;
    protected final String packageName;

    private TypeSpec.Kind classType;

    protected List<JavaFile> filesToGenerate = new ArrayList<>();
    protected TypeSpec.Builder classBuilder = getClassType(classType);

    protected AbstractCodeGenerator(@NotNull String className, @NotNull String packageName, TypeSpec.Kind classType) {
        this.className = className;
        this.packageName = packageName;
        this.classType = classType;
    }

    protected AbstractCodeGenerator(@NotNull String className, @NotNull String packageName) {
        this(className, packageName, TypeSpec.Kind.CLASS);
    }

    /**
     * Writes a list of files to the given output folder.
     *
     * @param fileList     list of files to write
     * @param outputFolder the output folder
     */
    protected void writeFiles(@NotNull List<JavaFile> fileList, @NotNull Path outputFolder) {
        if (fileList.isEmpty()) {
            logger.info(EMPTY_FILES_TO_WRITE);
            return;
        }
        write(fileList, outputFolder);
    }

    /**
     * Writes the files to the given output folder.
     *
     * @param outputFolder the output folder
     */
    protected void writeFiles(@NotNull Path outputFolder) {
        if (filesToGenerate.isEmpty()) {
            logger.info(EMPTY_FILES_TO_WRITE);
            return;
        }
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
                logger.error("Failed to write file: {}", e.getMessage());
            }
        });
    }

    @Override
    public void cleanup() {
        this.filesToGenerate.clear();
        this.classBuilder = getClassType(this.classType);
    }

    protected String changeFormat(String name) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name);
    }

    protected abstract List<T> getModels();

    private @NotNull TypeSpec.Builder getClassType(@NotNull TypeSpec.Kind classType) {
        return switch (classType) {
            case CLASS -> TypeSpec.classBuilder(className);
            case INTERFACE -> TypeSpec.interfaceBuilder(className);
            case ENUM -> TypeSpec.enumBuilder(className);
            case ANNOTATION -> TypeSpec.annotationBuilder(className);
        };
    }
}
