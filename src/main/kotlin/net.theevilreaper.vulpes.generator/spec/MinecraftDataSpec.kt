package net.theevilreaper.vulpes.generator.spec

import net.theevilreaper.vulpes.model.EnchantmentWrapper
import net.theevilreaper.vulpes.model.MaterialWrapper

/**
 * The spec is data object which contains several data about minecraft data from a specific version.
 * @param version the given minecraft version
 * @param materials a [List] which contains [MaterialWrapper] objects
 * @param enchantments a [List] which contains [EnchantmentWrapper] objects
 */
data class MinecraftDataSpec(
    val version: String,
    val materials: List<MaterialWrapper> = emptyList(),
    val enchantments: List<EnchantmentWrapper> = emptyList()
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MinecraftDataSpec
        return version == other.version
    }

    override fun hashCode(): Int {
        return version.hashCode()
    }
}