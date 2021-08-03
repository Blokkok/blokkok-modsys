package com.blokkok.modsys.communication.objects

import com.blokkok.modsys.communication.BroadcastCommunication

class Subscription(
    val handler: (List<Any?>) -> Unit,
    private val broadcast: BroadcastCommunication
) {
    var subscribed = true
        private set

    fun unsubscribe() {
        broadcast.subscribers.remove(this)
        subscribed = false
    }
}