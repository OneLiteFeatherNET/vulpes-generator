package net.theevilreaper.vulpes.generator.spec

import net.theevilreaper.vulpes.api.model.EnchantmentWrapper
import net.theevilreaper.vulpes.api.model.MaterialWrapper
import org.jetbrains.annotations.ApiStatus

/**
 * The spec is data object which contains several data about minecraft data from a specific version.
 * @param version the given minecraft version
 * @param materials a [List] which contains [MaterialWrapper] objects
 * @param enchantments a [List] which contains [EnchantmentWrapper] objects
 */
@ApiStatus.Internal
data class MinecraftDataSpec(
    val version: String,
    val materials: List<MaterialWrapper> = emptyList(),
    val enchantments: List<EnchantmentWrapper> = emptyList()
) {

    init {
        require(version.trim().isNotEmpty()) { "The version must not be empty" }
    }

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