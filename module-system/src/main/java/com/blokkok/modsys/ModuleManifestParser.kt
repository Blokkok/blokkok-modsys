package com.blokkok.modsys

import com.blokkok.modsys.exceptions.IncompatibleModSysVersion
import com.blokkok.modsys.models.ModuleManifest
import com.blokkok.modsys.models.ModuleMetadata
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

object ModuleManifestParser {
    @Throws(IncompatibleModSysVersion::class)
    fun parseManifest(manifest: String, ignoreLibraryVersion: Boolean = false): ModuleMetadata {
        val metadata = Json.decodeFromString<ModuleManifest>(manifest)

        if (
            metadata.libraryVer != VERSION &&   // check if the version is different
            !ignoreLibraryVersion &&            // check if we can ignore this
            !COMPATIBLE_WITH.contains(VERSION)  // check if this version is compatible
        ) {
            // this module is incompatible!
            throw IncompatibleModSysVersion(metadata.name, metadata.version, VERSION)
        }

        return metadata.let {
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
}
