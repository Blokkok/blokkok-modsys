package com.blokkok.modsys.communication

import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Subscription

data class BroadcastCommunication(
    val broadcaster: Broadcaster,
    val subscribers: ArrayList<Subscription> = ArrayList(),
) : Communication() {
    override val name: String
        get() = throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
}