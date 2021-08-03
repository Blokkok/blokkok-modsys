package com.blokkok.modsys.communication.objects

import com.blokkok.modsys.communication.BroadcastCommunication

class Subscription(
    val handler: (List<Any?>) -> Unit,
    private val broadcast: BroadcastCommunication
) {
    var subscribed = true
        private set

    fun unsubscribe() {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }
}