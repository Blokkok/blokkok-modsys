package com.blokkok.modsys.communication

import com.blokkok.modsys.communication.models.Broadcaster
import com.blokkok.modsys.communication.models.Subscription

data class BroadcastCommunication(
    val broadcaster: Broadcaster,
    val subscribers: ArrayList<Subscription> = ArrayList(),
) : Communication() {
    override val name: String = "broadcast"
}