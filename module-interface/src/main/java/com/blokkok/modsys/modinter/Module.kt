package com.blokkok.modsys.modinter

import com.blokkok.modsys.communication.CommunicationContext

abstract class Module {
    abstract val namespace: String
    open val flags: List<String> = emptyList()

    /**
     * This function will be called when this current module is loaded
     */
    abstract fun onLoaded(comContext: CommunicationContext)

    /**
     * This function will be called before this current module is unloaded
     */
    abstract fun onUnloaded(comContext: CommunicationContext)
}