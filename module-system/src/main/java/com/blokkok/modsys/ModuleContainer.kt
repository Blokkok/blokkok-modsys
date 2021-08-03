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
    val namespaceName = moduleInst.namespace

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
        for (flag in moduleInst.flags) {
            ModuleFlagsManager.putFlag(flag, this)
        }

        onLoaded.invoke(moduleInst, communicationContext)
    }

    fun unload() {
        onUnloaded.invoke(moduleInst, communicationContext)
        namespace.communications.clear()
    }
}