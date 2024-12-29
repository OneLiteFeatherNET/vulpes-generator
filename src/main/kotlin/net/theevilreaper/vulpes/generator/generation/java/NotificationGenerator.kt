package net.theevilreaper.vulpes.generator.generation.java

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import jakarta.inject.Singleton
import net.kyori.adventure.text.Component
import net.minestom.server.advancements.Advancement
import net.minestom.server.advancements.FrameType
import net.minestom.server.item.Material
import net.theevilreaper.vulpes.api.model.NotificationModel
import net.theevilreaper.vulpes.api.repository.NotificationRepository
import net.theevilreaper.vulpes.api.util.hasDescription
import net.theevilreaper.vulpes.api.util.hasTitle
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.util.BASE_PACKAGE
import net.theevilreaper.vulpes.generator.util.JavaGenerationHelper
import net.theevilreaper.vulpes.generator.util.INDENT_DEFAULT
import java.nio.file.Path

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@Singleton
class NotificationGenerator(
    val notificationRepository: NotificationRepository,
) : JavaGenerationHelper, BaseGenerator<NotificationModel>(
    className = "NotificationRegistry",
    packageName = "$BASE_PACKAGE.notification",
) {

    override fun generate(javaPath: Path) {
        val models = getModels()

        if (models.isEmpty()) return
        this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classSpec)
        addJetbrainsAnnotation(this.classSpec)
        addPrivateDefaultConstructor(this.classSpec)
        addSuppressAnnotation(this.classSpec)
        val fields = getFields(models).values
        this.classSpec.addFields(fields)
        val javaFile = JavaFile.builder(packageName, this.classSpec.build())
            .indent(INDENT_DEFAULT)
            .skipJavaLangImports(true)
            .build();
        writeFiles(listOf(javaFile), javaPath)
    }

    /**
     * Returns the name from the generator.
     * @return the given name
     */
    override fun getName(): String = "NotificationGenerator"

    override fun getModels(): List<NotificationModel> = notificationRepository.findAll()

    private fun getFields(models: List<NotificationModel>): Map<String, FieldSpec> {
        val advancements: MutableMap<String, FieldSpec> = mutableMapOf()
        models.filter { it.name.orEmpty().trim().isNotEmpty() }.forEach { model ->
            if (advancements.isNotEmpty() && advancements.containsKey(model.name)) return@forEach
            val frame = FrameType.valueOf(model.frameType!!.uppercase())
            val material = if (model.material.isNullOrEmpty()) {
                Material.STONE
            } else {
                Material.fromNamespaceId(model.material!!) ?: Material.STONE
            }

            val field = FieldSpec.builder(
                Advancement::class.java, model.name!!.uppercase()
            )
                .addModifiers(*defaultModifiers)
                .initializer(
                    CodeBlock.of(
                        "new \$T(\$T.\$L, Component.\$L, Material.\$L, \$T.\$L, \$L, \$L)",
                        Advancement::class.java,
                        Component::class.java,
                        if (model.hasTitle()) getTextContent(model.title!!) else emptyComponent,
                        if (model.hasDescription()) getTextContent(model.description!!) else emptyComponent,
                        material.namespace().path().uppercase(),
                        FrameType::class.java,
                        frame,
                        0,
                        0
                    )
                ).build()
            advancements[model.name!!] = field
        }

        return advancements
    }

    private fun getTextContent(input: String): String {
        return "text(\"$input\")"
    }
}
