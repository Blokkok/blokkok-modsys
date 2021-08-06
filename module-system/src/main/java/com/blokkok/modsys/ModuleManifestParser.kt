package com.blokkok.modsys

import com.blokkok.modsys.models.ModuleManifest
import com.blokkok.modsys.models.ModuleMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object ModuleManifestParser {
    fun parseManifest(manifest: String): ModuleMetadata =
        Json.decodeFromString<ModuleManifest>(manifest).let {
            ModuleMetadata(
                it.id,
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
