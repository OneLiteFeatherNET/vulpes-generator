package net.theevilreaper.vulpes.generator.util

import com.google.common.base.CaseFormat
import java.util.regex.Pattern


/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

val DOUBLE_DOT: Pattern = Pattern.compile(":")
const val INDENT_DEFAULT = "   ";
const val MINECRAFT_KEY = "minecraft:"
const val BASE_PACKAGE = "net.reaper.vulpes"

const val ITEM_CONST = "ITEM_"

fun String.toVariableString() = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, this)
