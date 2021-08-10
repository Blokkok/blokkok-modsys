package com.blokkok.modsys

import com.blokkok.modsys.exceptions.IncompatibleModSysVersion
import com.blokkok.modsys.models.ModuleManifest
import com.blokkok.modsys.models.ModuleMetadata

object ModuleManifestParser {
    @Throws(IncompatibleModSysVersion::class)
    fun parseManifest(manifest: ModuleManifest, ignoreLibraryVersion: Boolean = false): ModuleMetadata {
        if (
            manifest.libraryVer != VERSION &&   // check if the version is different
            !ignoreLibraryVersion &&            // check if we can ignore this
            !COMPATIBLE_WITH.contains(VERSION)  // check if this version is compatible
        ) {
            // this module is incompatible!
            throw IncompatibleModSysVersion(manifest.name, manifest.version, VERSION)
        }

        return manifest.let {
            ModuleMetadata(
                it.id,
                it.name,
                it.description,
                it.version,
                it.author,
                it.website,
                it.classpath,
                false,
                it.jar,
                it.assetsFolder,
                it.dependencies
            )
        }
    }
}
