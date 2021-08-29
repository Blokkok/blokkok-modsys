package com.blokkok.modsys.communication

data class ExtensionPointCommunication(
    val spec: Class<*>,
    val implementors: ArrayList<Any> = ArrayList(),
) : Communication() {
    override val name: String = "extension point"
}