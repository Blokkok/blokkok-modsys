package com.blokkok.modsys.communication

data class StreamCommunication(
    val entryHandler: Stream.() -> Unit
) : Communication() {
    override val name: String = "stream"
}