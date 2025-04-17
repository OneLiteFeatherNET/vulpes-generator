package net.theevilreaper.vulpes.generator.generation;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.TypeSpec;
import net.theevilreaper.vulpes.api.model.VulpesModel;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link AbstractCodeGenerator} represents a new layer of abstraction for the {@link AbstractGenerator}.
 * It can be used to generate code for specific models via the JavaPoet library.
 *
 * @param <T> the type of the model to be generated
 * @author theEvilReaper
 * @version 1.0.0
 * @see AbstractGenerator
 * @since 1.0.0
 */
public abstract class AbstractCodeGenerator<T extends VulpesModel> extends AbstractGenerator<T> {

    protected static final String BASE_PACKAGE = "net.reaper.vulpes";
    protected static final String EMPTY_COMPONENT = "empty()";

    protected final String className;
    protected final String packageName;

    protected TypeSpec.Builder classBuilder;

    private final TypeSpec.Kind classType;

    /**
     * Creates a new instance of the {@link AbstractCodeGenerator} class.
     *
     * @param className   the name of the class to be generated
     * @param packageName the package name of the class to be generated
     * @param classType   the type of the class to be generated
     */
    protected AbstractCodeGenerator(@NotNull String className, @NotNull String packageName, TypeSpec.Kind classType) {
        this.className = className;
        this.packageName = packageName;
        this.classType = classType;
        this.classBuilder = getClassType(classType);
    }

    /**
     * Creates a new instance of the {@link AbstractCodeGenerator} class.
     *
     * @param className   the name of the class to be generated
     * @param packageName the package name of the class to be generated
     */
    protected AbstractCodeGenerator(@NotNull String className, @NotNull String packageName) {
        this(className, packageName, TypeSpec.Kind.CLASS);
    }

    @Override
    public void cleanup() {
        super.cleanup();
        this.classBuilder = getClassType(this.classType);
    }

    /**
     * Changes the format of the given input to upper underscore.
     *
     * @param input the input to change
     * @return the changed input
     */
    protected @NotNull String changeFormat(@NotNull String input) {
        if (input.trim().isEmpty()) return input;
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, input);
    }

    /**
     * Returns the {@link TypeSpec} instance which refers to the given {@link TypeSpec.Kind}.
     *
     * @param classType the class type
     * @return the {@link TypeSpec} instance
     */
    private @NotNull TypeSpec.Builder getClassType(@NotNull TypeSpec.Kind classType) {
        return switch (classType) {
            case CLASS -> TypeSpec.classBuilder(className);
            case INTERFACE -> TypeSpec.interfaceBuilder(className);
            case ENUM -> TypeSpec.enumBuilder(className);
            case ANNOTATION -> TypeSpec.annotationBuilder(className);
        };
    }
}
