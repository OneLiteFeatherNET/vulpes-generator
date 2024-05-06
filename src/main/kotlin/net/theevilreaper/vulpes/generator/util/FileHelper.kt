package net.theevilreaper.vulpes.generator.util

import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream


/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/
object FileHelper {

    fun zipFile(temp: Path, zipFile: Path) {
        ZipOutputStream(zipFile.outputStream()).use { zos ->
            val tempFile = temp.toFile()
            val filteredFiles = tempFile.walkTopDown().filter { it.absolutePath != tempFile.absolutePath }
                .filter { !it.name.endsWith(".zip") }
            filteredFiles.forEach { file ->
                val zipFileName = file.absolutePath.removePrefix(tempFile.absolutePath).removePrefix(File.separator)
                val entry = ZipEntry("$zipFileName${(if (file.isDirectory) "/" else EMPTY_STRING)}")
                zos.putNextEntry(entry)
                if (file.isFile) {
                    file.inputStream().copyTo(zos)
                }
            }
        }
    }
}
