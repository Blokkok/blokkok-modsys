package com.blokkok.modsys.communication

import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Subscription
import com.blokkok.modsys.modinter.exception.FlagAlreadyClaimedException
import com.blokkok.modsys.modinter.exception.IllegalFlagAccessException
import com.blokkok.modsys.namespace.Namespace

/**
 * Communication API entry point
 */
class CommunicationContext(
    private val namespace: Namespace
) {
    fun namespace(name: String, block: CommunicationContext.() -> Unit) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Functions ===================================================================================

    fun createFunction(name: String, handler: (List<Any?>) -> Any?) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun invokeFunction(name: String, args: List<Any?> = emptyList()): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun invokeFunction(namespace: String, name: String, args: List<Any?> = emptyList()): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Broadcast ===================================================================================

    fun createBroadcaster(name: String): Broadcaster {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun subscribeToBroadcast(name: String, handler: (List<Any?>) -> Unit): Subscription {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun subscribeToBroadcast(namespace: String, name: String, handler: (List<Any?>) -> Unit): Subscription {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Flags =======================================================================================

    fun claimFlag(flagName: String) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    fun getFlagNamespaces(flagName: String): List<String> {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }
}