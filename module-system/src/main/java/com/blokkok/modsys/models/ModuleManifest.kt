package com.blokkok.modsys.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModuleManifest(
    val id: String,
    val name: String,
    @SerialName("library_ver") val libraryVer: String,
    val description: String,
    val version: String,
    val author: String,
    val classpath: String,
    val jar: String,
    val website: String? = null,
)
