package net.theevilreaper.vulpes.generator.generation.dart.material

/**
 * Represents the different material categories which comes from the game Minecraft.
 * The game represents the categories only as boolean values.
 * @param type the type of the category
 * @version 1.0.0
 * @since 1.0.0
 * @property type the type of the category
 */
enum class MaterialSubType(val type: String) {

    BLOCK("block"),
    FOOD("food"),
    ARMOR("armor"),
}
