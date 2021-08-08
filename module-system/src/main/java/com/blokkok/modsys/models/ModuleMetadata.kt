package com.blokkok.modsys.models

import kotlinx.serialization.Serializable

// This is the json structure used in the imported modules, not the "un-extracted modules"
@Serializable
data class ModuleMetadata(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val website: String? = null,
    val classpath: String,
    val enabled: Boolean,
    val jarPath: String,
    val dependencies: List<String> = emptyList(),
)
