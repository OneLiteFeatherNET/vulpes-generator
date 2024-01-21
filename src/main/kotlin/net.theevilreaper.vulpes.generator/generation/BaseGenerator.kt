package net.theevilreaper.vulpes.generator.generation

import com.google.common.base.CaseFormat
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeSpec.Kind
import net.theevilreaper.vulpes.generator.generation.doc.ClassDocumentation
import net.theevilreaper.vulpes.generator.generation.type.GeneratorType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Path
import javax.lang.model.element.Modifier

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
abstract class BaseGenerator<T>(
    val className: String,
    val packageName: String,
    private val classType: Kind = Kind.CLASS,
    val generatorType: GeneratorType = GeneratorType.JAVA
) : Generator, ClassDocumentation {

    private val logger: Logger = LoggerFactory.getLogger(BaseGenerator::class.java)
    protected val defaultModifiers = arrayOf(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
    protected val filesToGenerate: MutableList<JavaFile> = arrayListOf()
    protected val emptyComponent = "empty()"
    protected var classSpec: TypeSpec.Builder = getClassType(classType)

    protected fun writeFiles(filesList: List<JavaFile>, outputFolder: Path) {
        if (filesList.isEmpty()) {
            logger.info("No files to write. Skipping the file write")
            return
        }
        write(filesList, outputFolder)
    }

    protected fun writeFiles(outputFolder: Path) {
        if (filesToGenerate.isEmpty()) {
            logger.info("No files to write. Skipping the file write")
            return
        }
        write(filesToGenerate, outputFolder)
        filesToGenerate.clear()
    }

    private fun write(files: List<JavaFile>, outputFolder: Path) {
        files.forEach { file ->
            try {
                file.writeTo(outputFolder)
            } catch (exception: IOException) {
                logger.warn("An error occurred while writing source code to the file system: {0}", exception)
            }
        }
    }

    private fun getClassType(classType: Kind): TypeSpec.Builder {
        return when (classType) {
            Kind.ENUM -> TypeSpec.enumBuilder(className)
            Kind.ANNOTATION -> TypeSpec.annotationBuilder(className)
            Kind.INTERFACE -> TypeSpec.interfaceBuilder(className)
            Kind.CLASS -> TypeSpec.classBuilder(className)
            else -> throw IllegalArgumentException("No classType found. This should never happened")
        }
    }

    /**
     * Clears the internal file cache.
     */
    override fun cleanUp() {
        this.filesToGenerate.clear()
        this.classSpec = getClassType(classType)
    }

    fun toConstantVarDeclaration(identifier: String?, name: String): String {
        return if (identifier.isNullOrEmpty()) changeFormat(name) else identifier + changeFormat(name)
    }

    private fun changeFormat(name: String): String = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, name)

    /**
     * Contains the logic of what happens during the generation.
     * @param javaPath the [Path] where the files should be generated
     */
    abstract override fun generate(javaPath: Path)

    /**
     * Returns the name from the generator.
     * @return the given name as string
     */
    abstract override fun getName(): String

    /**
     * Returns a list which contains all models from a database.
     * @return a [List] which contains all models
     */
    internal abstract fun getModels(): List<T>

    override fun getType(): GeneratorType = generatorType
}
