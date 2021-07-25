package com.blokkok.modsys

import com.blokkok.modsys.models.ModuleManifest
import com.blokkok.modsys.models.ModuleMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.lang.StringBuilder

object ModuleManifestParser {
    fun parseManifest(manifest: String): ModuleMetadata =
        Json.decodeFromString<ModuleManifest>(manifest).let {
            ModuleMetadata(
                it.name.toId(),
                it.name,
                it.description,
                it.version,
                it.author,
                it.website,
                it.classpath,
                enabled = false,
                jarPath = it.jar
            )
        }
}

private fun String.toId(): String =
    StringBuilder().apply {
        this@toId.forEach {
            if (!(it.isLetter()
                        && it.category != CharCategory.MODIFIER_LETTER
                        && it.category != CharCategory.OTHER_LETTER)
            ) {
                append('_')
            } else append(it)
        }
    }.toString()
