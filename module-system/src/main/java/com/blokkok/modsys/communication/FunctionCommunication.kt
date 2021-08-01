package com.blokkok.modsys.communication

data class FunctionCommunication(
    val handler: (List<String>) -> Unit,
) : Communication() {
    override val name: String = "function"
}