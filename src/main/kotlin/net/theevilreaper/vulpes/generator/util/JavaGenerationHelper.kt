package net.theevilreaper.vulpes.generator.util

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeSpec
import org.jetbrains.annotations.ApiStatus
import javax.lang.model.element.Modifier

/**
 * The [JavaGenerationHelper] interface contains some methods which are used to reduce boilerplate code parts
 * in the generation process of the code for Java.
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@ApiStatus.Internal
internal interface JavaGenerationHelper {

    val constructorDocumentation: CodeBlock
        get() = CodeBlock.builder()
            .addStatement("\$L", "Default constructor for the class")
            .add("\$L", "It's private because the class should not be instantiated")
            .build()

    val jetbrainsAnnotation: Class<out Annotation> get() = ApiStatus.NonExtendable::class.java
    private val defaultSuppressWarnings: Set<String> get() = setOf("java:S3252")

    /**
     * Add a private default constructor to the given [TypeSpec.Builder].
     * @param spec the spec where the constructor should be added
     * @return the [TypeSpec.Builder] with the added constructor
     */
    fun addPrivateDefaultConstructor(spec: TypeSpec.Builder): TypeSpec.Builder {
        spec.addMethod(
            MethodSpec.constructorBuilder()
                .addJavadoc(constructorDocumentation)
                .addModifiers(Modifier.PRIVATE)
                .addComment("Nothing to do here")
                .build()
        )
        return spec
    }

    /**
     * Adds the default class modifiers which each class should have after the generation process.
     * It emits the [Modifier.PUBLIC] and [Modifier.FINAL] modifier combination to prevent class inheritance.
     * @param spec the spec where the modifiers should be added
     * @return the [TypeSpec.Builder] with the added modifiers
     */
    fun addClassModifiers(spec: TypeSpec.Builder): TypeSpec.Builder = spec.addModifiers(Modifier.PUBLIC, Modifier.FINAL)

    /**
     * Adds the [ApiStatus.Experimental] annotation to the given [TypeSpec.Builder].
     * @param spec the spec where the annotation should be added
     * @return the [TypeSpec.Builder] with the added annotation
     */
    fun addJetbrainsAnnotation(spec: TypeSpec.Builder): TypeSpec.Builder = spec.addAnnotation(jetbrainsAnnotation)

    fun addSuppressAnnotation(spec: TypeSpec.Builder, warnings: Set<String> = defaultSuppressWarnings): TypeSpec.Builder {
        spec.addAnnotation(
            AnnotationSpec.builder(SuppressWarnings::class.java)
                .addMember("value", warnings.joinToString(prefix = "{", separator = ",", postfix = "}") { "\"$it\"" })
                .build()
        )
        return spec
    }
}
