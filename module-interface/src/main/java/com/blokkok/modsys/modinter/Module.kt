package com.blokkok.modsys.modinter

import com.blokkok.modsys.communication.CommunicationContext
import java.io.File

abstract class Module {
    abstract val namespace: String
    open val flags: List<String> = emptyList()

    /**
     * The File object pointing to the extracted assets folder
     */
    protected var assets: File? = null

    /**
     * This function will be called when this current module is loaded
     */
    abstract fun onLoaded(comContext: CommunicationContext)

    /**
     * This function will be called before this current module is unloaded
     */
    abstract fun onUnloaded(comContext: CommunicationContext)
}