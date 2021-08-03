package com.blokkok.modsys.communication.objects

import java.util.*
import java.util.concurrent.locks.ReentrantLock

class Stream {
    val buffer = LinkedList<Any?>()

    var closed = false
        private set

    private var otherStream: Stream? = null
    private var sendListener: ((Any?) -> Unit)? = null

    private val lock = ReentrantLock()
    private val recvCondition = lock.newCondition()

    /**
     * Connects this stream to an other stream
     */
    internal fun connectTo(otherStream: Stream) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    private fun listenSend(listener: (Any?) -> Unit) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Sends a data into the other stream (if this stream is connected)
     */
    fun send(data: Any?) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Receives a data from the other stream
     * will block the current thread until the other stream sent the data
     */
    fun recvBlock(): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Receives data in a callback
     */
    fun recvCallback(handler: (Any?) -> Unit) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Closes the current stream & the other stream if this has been connected
     */
    fun close() {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }
}