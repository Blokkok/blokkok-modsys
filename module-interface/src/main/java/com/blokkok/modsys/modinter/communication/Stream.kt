package com.blokkok.modsys.modinter.communication

import java.io.IOException
import java.util.*

abstract class Stream {
    val ingoingBuffer = Stack<Any>()

    abstract fun send(value: Any)

    @Throws(IOException::class)
    abstract fun receive(): Any

    abstract fun receiveCallback(callback: (Any) -> Unit)
}