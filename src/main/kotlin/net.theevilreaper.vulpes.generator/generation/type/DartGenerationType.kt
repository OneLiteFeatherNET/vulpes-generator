package net.theevilreaper.vulpes.generator.generation.type

/**
 * The enum contains all special generation types for the programming language Dart.
 * In this case "special" means that the generation doesn't rely on the Minestom codebase.
 * For this case the program loads some data from an external source and generates the code
 * @since 1.0.0
 * @author theEvilReaper
 */
enum class DartGenerationType(
    val type: String
) {
    MATERIAL("Material"),
    ENCHANTMENT("Enchantment"),
    DEFAULT("default");

    /**
     * The companion object contains some helper methods for the [DartGenerationType].
     */
    companion object {

        private val default = DEFAULT

        /**
         * Returns the [DartGenerationType] which matches the given type as [String].
         * @param type The type which should be checked
         * @return The [DartGenerationType] or [DartGenerationType.DEFAULT] if no match was found
         */
        fun getGenerationType(type: String): DartGenerationType {
            return entries.firstOrNull { it.type == type } ?: default
        }
    }
}
