package net.theevilreaper.vulpes.generator.util

object StringHelper {

    fun mapDisplayName(rawName: String): String {

        if (!rawName.contains("_")) {
            return rawName.lowercase().replaceFirstChar { it.uppercase() }
        }

        return rawName.split("_").joinToString(separator = SPACE_STRING) {
            it.lowercase().replaceFirstChar {
                char -> char.uppercase()
            }
        }
    }
}