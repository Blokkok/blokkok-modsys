package com.blokkok.modsys.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModuleManifest(
    /**
     * ID used to differentiate with different modules
     */
    val id: String,

    /**
     * Module display name
     */
    val name: String,

    /**
     * The modsys library version used to make this module
     */
    @SerialName("library_ver")
    val libraryVer: String,

    /**
     * The description of this library
     */
    val description: String,

    /**
     * The version of this module
     */
    val version: String,

    /**
     * The author of this module
     */
    val author: String,

    /**
     * The classpath where the module class will be located inside the jar
     */
    val classpath: String,

    /**
     * The jarfile where the module class is located
     */
    val jar: String,

    /**
     * A list of other module names this module need to run
     *
     * The module name must include it's version separated with `:`
     *
     * Example:
     * ```
     * "dependsOn": ["essentials:1.0.0", "toaster:3.52-44725"]
     * ```
     */
    @SerialName("dependsOn")
    val dependencies: List<String> = emptyList(),

    /**
     * Website link (if any)
     */
    val website: String? = null,
)
