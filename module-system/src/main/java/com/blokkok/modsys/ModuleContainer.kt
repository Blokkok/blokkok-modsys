package com.blokkok.modsys

import com.blokkok.modsys.annotations.processor.ModuleRuntimeAnnotationProcessor
import com.blokkok.modsys.communication.CommunicationContext
import com.blokkok.modsys.models.ModuleMetadata
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.communication.namespace.Namespace
import com.blokkok.modsys.communication.namespace.NamespaceResolver

/**
 * Class that contains a module instance
 */
class ModuleContainer(
    private val moduleInst: Module,
    moduleMetadata: ModuleMetadata,
) {
    val namespaceName = moduleInst.namespace

    private val namespace = NamespaceResolver.newNamespace(
        "/",
        Namespace(
            if (!moduleInst.namespace.isCommunicationName())
                throw IllegalArgumentException("Namespace name can only be alphanumeric plus -+_")
            else
                moduleInst.namespace
        )
    )

    private val communicationContext = CommunicationContext(namespace)

    // We can't call these functions normally for some reason
    private val onLoaded = Module::class.java
        .getMethod("onLoaded", CommunicationContext::class.java)

    private val onUnloaded = Module::class.java
        .getMethod("onUnloaded", CommunicationContext::class.java)

    private val onAllLoaded = Module::class.java
        .getMethod("onAllLoaded", CommunicationContext::class.java)

    init {
        for (flag in moduleInst.flags) {
            ModuleFlagsManager.putFlag(flag, this)
        }

        // Set the assets variable if it exists
        if (moduleInst::class.java.fields.find { it.name == "assets" } != null) {
            val assetsVariable = moduleInst::class.java.getField("assets")
            ModuleManager.getAssetsFolder(moduleMetadata.id)?.let { assetsFolder ->
                assetsVariable.isAccessible = true
                assetsVariable.set(moduleInst, assetsFolder)
            }
        }

        // process modsys annotations, this function will add the communications on the passed
        // namespace.communications map
        ModuleRuntimeAnnotationProcessor
            .process(
                moduleInst,
                moduleInst::class.java,
                namespace.communications
            )

        onLoaded.invoke(moduleInst, communicationContext)
    }

    fun callOnAllLoaded() {
        onAllLoaded.invoke(moduleInst, communicationContext)
    }

    fun unload() {
        onUnloaded.invoke(moduleInst, communicationContext)
        namespace.communications.clear()
    }
}