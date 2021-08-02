package com.blokkok.modsys

import com.blokkok.modsys.communication.CommunicationContext
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.namespace.Namespace
import com.blokkok.modsys.namespace.NamespaceResolver

/**
 * Class that contains a module instance
 */
class ModuleContainer(
    private val moduleInst: Module
) {
    private val namespace = NamespaceResolver.newNamespace(
        "/",
        Namespace(moduleInst.namespace)
    )

    private val communicationContext = CommunicationContext(namespace)

    // We can't call these functions normally for some reason
    private val onLoaded = Module::class.java
        .getMethod("onLoaded", CommunicationContext::class.java)

    private val onUnloaded = Module::class.java
        .getMethod("onUnloaded", CommunicationContext::class.java)

    init {
        onLoaded.invoke(moduleInst, communicationContext)
    }

    fun unload() {
        onUnloaded.invoke(moduleInst, communicationContext)
        namespace.communications.clear()
    }
}