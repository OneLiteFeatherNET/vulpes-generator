package net.theevilreaper.vulpes.generator.util

import net.theevilreaper.dartpoet.DartModifier
import net.theevilreaper.dartpoet.parameter.ParameterSpec
import net.theevilreaper.dartpoet.property.PropertySpec

internal val CLASS_PROPERTIES: Array<PropertySpec> = arrayOf(
    PropertySpec.builder("name", String::class).build(),
    PropertySpec.builder("category", String::class).build(),
    PropertySpec.builder("minLevel", Integer::class).build(),
    PropertySpec.builder("maxLevel", Integer::class).build()
)

internal val CONSTRUCTOR_PARAMETERS: Array<ParameterSpec> = arrayOf(
    ParameterSpec.builder("name").modifier(DartModifier.FINAL).build(),
    ParameterSpec.builder("category").modifier(DartModifier.FINAL).build(),
    ParameterSpec.builder("minLevel").modifier(DartModifier.FINAL).build(),
    ParameterSpec.builder("maxLevel").modifier(DartModifier.FINAL).build()
)