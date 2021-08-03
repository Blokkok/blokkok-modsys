package com.blokkok.modsys.communication.objects

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
        if (this.otherStream != null) {
            // Remove the sendListener
            this.otherStream!!.sendListener = null
        }

        this.otherStream = otherStream
        this.otherStream!!.listenSend {
            buffer.push(it)
            recvCondition.signalAll()
        }
    }

    private fun listenSend(listener: (Any?) -> Unit) { sendListener = listener }

    /**
     * Sends a data into the other stream (if this stream is connected)
     */
    fun send(data: Any?) {
        sendListener?.let { it(data) }
    }

    /**
     * Receives a data from the other stream
     * will block the current thread until the other stream sent the data
     */
    fun recvBlock(): Any? {
        lock.withLock {
            while (buffer.isEmpty()) {
                recvCondition.await()
            }

            return buffer.pop()
        }
    }

    /**
     * Receives data in a callback
     */
    fun recvCallback(handler: (Any?) -> Unit) {
        Thread {
            handler(
                recvBlock()
            )
        }.start()
    }

    /**
     * Closes the current stream & the other stream if this has been connected
     */
    fun close() {
        closed = true
        otherStream?.closed = true
    }
}