package com.blokkok.modsys.modinter

import com.blokkok.modsys.communication.CommunicationContext

abstract class Module {
    abstract val namespace: String

    abstract fun onLoaded(comContext: CommunicationContext)
    abstract fun onUnloaded(comContext: CommunicationContext)
}