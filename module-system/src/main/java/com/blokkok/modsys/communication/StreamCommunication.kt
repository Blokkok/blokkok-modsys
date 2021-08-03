package com.blokkok.modsys.communication

import com.blokkok.modsys.communication.objects.Stream

data class StreamCommunication(
    val entryHandler: Stream.() -> Unit
) : Communication() {
    override val name: String = "stream"
}