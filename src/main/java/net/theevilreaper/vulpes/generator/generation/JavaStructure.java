package net.theevilreaper.vulpes.generator.generation;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

public interface JavaStructure {

    Set<String> DEFAULT_SUPPRESS_WARNINGS = Set.of("java:S3252");

    /**
     * Add a private default constructor to the given {@link TypeSpec.Builder}.
     *
     * @param spec the spec where the constructor should be added
     */
    default void addPrivateDefaultConstructor(@NotNull TypeSpec.Builder spec) {
        spec.addMethod(
                MethodSpec.constructorBuilder()
                        //.addJavadoc(constructorDocumentation)
                        .addModifiers(Modifier.PRIVATE)
                        .addComment("Nothing to do here")
                        .build()
        );
    }

    /**
     * Adds the default class modifiers which each class should have after the generation process.
     * It emits the [Modifier.PUBLIC] and [Modifier.FINAL] modifier combination to prevent class inheritance.
     *
     * @param spec the spec where the modifiers should be added
     */
    default void addClassModifiers(@NotNull TypeSpec.Builder spec) {
        spec.addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    }

    /**
     * Adds the {@link org.jetbrains.annotations.ApiStatus.NonExtendable} annotation to the given {@link TypeSpec.Builder}.
     *
     * @param spec the spec where the annotation should be added
     */
    default void addJetbrainsAnnotation(@NotNull TypeSpec.Builder spec) {
        spec.addAnnotation(ApiStatus.NonExtendable.class);
    }

    /**
     * Adds the default suppress annotation to the given {@link TypeSpec.Builder}.
     * This method adds the {@link SuppressWarnings} annotation with the default warnings.
     *
     * @param spec the spec where the annotation should be added
     */
    default void addDefaultSuppressAnnotation(@NotNull TypeSpec.Builder spec) {
        this.addSuppressAnnotation(spec, DEFAULT_SUPPRESS_WARNINGS);
    }

    /**
     * Adds the suppression annotation to the given {@link TypeSpec.Builder}.
     *
     * @param spec     the spec where the annotation should be added
     * @param warnings the warnings which should be suppressed
     */
    default void addSuppressAnnotation(@NotNull TypeSpec.Builder spec, @NotNull Set<String> warnings) {
        String arguments = warnings.stream().map(w -> "\"" + w + "\"").collect(Collectors.joining(",", "{", "}"));
        spec.addAnnotation(
                AnnotationSpec.builder(SuppressWarnings.class)
                        .addMember("value", arguments)
                        .build()
        );
    }
}
