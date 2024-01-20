package net.theevilreaper.vulpes.generator.generation.java

import com.squareup.javapoet.CodeBlock
import com.squareup.javapoet.FieldSpec
import net.reaper.vulpes.font.FontSymbol
import net.theevilreaper.vulpes.api.model.FontModel
import net.theevilreaper.vulpes.api.repository.FontRepository
import net.theevilreaper.vulpes.api.util.getCharsAsArray
import net.theevilreaper.vulpes.api.util.getShiftAsArray
import net.theevilreaper.vulpes.api.util.hasFontSymbols
import net.theevilreaper.vulpes.api.util.hasShiftData
import net.theevilreaper.vulpes.generator.generation.BaseGenerator
import net.theevilreaper.vulpes.generator.util.BASE_PACKAGE
import net.theevilreaper.vulpes.generator.util.JavaGenerationHelper
import net.theevilreaper.vulpes.generator.util.toVariableString
import org.springframework.stereotype.Service
import java.nio.file.Path

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
@Service
class FontGenerator(
    val fontRepository: FontRepository
) : JavaGenerationHelper, BaseGenerator<FontModel>(
    className = "FontRegistry",
    packageName = "$BASE_PACKAGE.font",
) {

    override fun generate(outputFolder: Path) {
        val models = getModels()

        if (models.isEmpty()) return

        val fieldSpecs = generateFonts(models).values
        this.classSpec.addJavadoc(defaultDocumentation)
        addClassModifiers(this.classSpec)
        addJetbrainsAnnotation(this.classSpec)
        addPrivateDefaultConstructor(this.classSpec)
        this.classSpec.addFields(fieldSpecs)
    }

    private fun generateFonts(models: List<FontModel>): Map<String, FieldSpec> {
        val generatedFonts: MutableMap<String, FieldSpec> = mutableMapOf()

        models.filter { it.name.orEmpty().isNotEmpty() }.forEach { model ->
            if (generatedFonts.isNotEmpty() && generatedFonts.containsKey(model.name)) return@forEach
            if (!model.hasFontSymbols()) return@forEach

            val fontCode = CodeBlock.builder()

            fontCode.add("\$T.builder()", FontSymbol::class.java)
            fontCode.add(".symbols(\$L)", model.getCharsAsArray())

            if (model.ascent != null) {
                fontCode.add(".ascent(\$L)", model.ascent!!)
            }

            if (model.height != null) {
                fontCode.add(".height(\$L)", model.height!!)
            }

            if (model.hasShiftData()) {
                fontCode.add(".shift(\$L)", model.getShiftAsArray())
            }

            val fieldValue = FieldSpec.builder(FontSymbol::class.java, model.name!!.toVariableString()).build()
            generatedFonts[model.name!!] = fieldValue

        }
        return generatedFonts
    }

    override fun getName(): String = "FontGenerator"

    override fun getModels(): List<FontModel> {
        return fontRepository.findAll()
    }
}