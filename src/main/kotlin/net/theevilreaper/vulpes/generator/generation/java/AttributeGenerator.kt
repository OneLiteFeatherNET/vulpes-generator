package net.theevilreaper.vulpes.generator.generation.java

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.minestom.server.attribute.Attribute
import net.theevilreaper.vulpes.api.model.AttributeModel
import net.theevilreaper.vulpes.api.repository.AttributeRepository
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.util.*
import net.theevilreaper.vulpes.generator.util.JavaGenerationHelper
import java.nio.file.Path
import javax.lang.model.element.Modifier

/**
 * The [AttributeGenerator] contains the [Attribute] generation logic to generate a class which
 * contains a specific amount of attribute constant values. Each attribute value are fetched from the database.
 * When the database contains no values the generator will skip the generation process.
 * @author theEvilReaper
 * @since 1.0.0
 */
@Singleton
class AttributeGenerator @Inject constructor(
    private val attributeRepository: AttributeRepository,
) : JavaGenerationHelper, BaseGenerator<AttributeModel>(
    className = "DungeonAttributes",
    packageName = "$BASE_PACKAGE.attribute",
) {

    private val attributeClass: Class<Attribute> = Attribute::class.java

    /**
     * Generates the [Attribute] class and writes it to the given [Path].
     * @param javaPath the path where the class should be written
     */
    override fun generate(javaPath: Path) {
        val models = getModels()

        if (models.isEmpty()) return

        val generatedModels: MutableMap<String, FieldSpec> = mutableMapOf()

        models.forEach {
            if (it.name == null || it.modelName == null) return@forEach
            val name = it.name ?: it.modelName ?: EMPTY_STRING

            if (name == EMPTY_STRING) return@forEach

            val field = FieldSpec.builder(attributeClass, name.uppercase())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("new \$T(\$S, \$Lf, \$Lf)", attributeClass, name, it.defaultValue, it.maximumValue)
                .build()
            generatedModels.putIfAbsent(name, field)
        }

        this.classSpec.addFields(generatedModels.values.toList())
        this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classSpec)
        addJetbrainsAnnotation(this.classSpec)
        addPrivateDefaultConstructor(this.classSpec)
        addSuppressAnnotation(this.classSpec)
        val javaFile = JavaFile.builder(packageName, this.classSpec.build())
            .indent(INDENT_DEFAULT)
            .skipJavaLangImports(true)
            .build()
        writeFiles(listOf(javaFile), javaPath)
    }

    /**
     * Returns the given name from the generator.
     * @return the name from the generator
     */
    override fun getName(): String = "AttributeGenerator"

    /**
     * Returns a list which contains all available [AttributeModel] from the database.
     * @return the list with all available models
     */
    override fun getModels(): List<AttributeModel> = attributeRepository.findAll()
}