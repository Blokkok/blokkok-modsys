package com.blokkok.modsys.communication

data class FunctionCommunication(
    val handler: (List<Any?>) -> Any?,
) : Communication() {
    override val name: String = "function"
}