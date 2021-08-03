package com.blokkok.modsys.modinter

import com.blokkok.modsys.communication.CommunicationContext

abstract class Module {
    abstract val namespace: String
    val flags: List<String> = emptyList()

    abstract fun onLoaded(comContext: CommunicationContext)
    abstract fun onUnloaded(comContext: CommunicationContext)
}