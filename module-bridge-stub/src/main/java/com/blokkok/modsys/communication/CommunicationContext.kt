package com.blokkok.modsys.communication

import com.blokkok.modsys.namespace.Namespace
import java.lang.RuntimeException

/**
 * Communication API entry point
 */
class CommunicationContext(
    private val namespace: Namespace
) {
    fun createFunction(name: String, handler: (List<Any?>) -> Any?) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun invokeFunction(name: String, args: List<Any?>): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun invokeFunction(namespace: String, name: String, args: List<Any?>): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun namespace(name: String, block: CommunicationContext.() -> Unit) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }
}