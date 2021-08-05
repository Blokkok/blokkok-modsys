package com.blokkok.modsys.communication

data class FunctionCommunication(
    val handler: (Map<String, Any>) -> Any?,
) : Communication() {
    override val name: String = "function"
}