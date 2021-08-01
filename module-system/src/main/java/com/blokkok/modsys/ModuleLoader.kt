package com.blokkok.modsys

import android.util.Log
import com.blokkok.modsys.communication.CommunicationContext
import com.blokkok.modsys.models.ModuleMetadata
import com.blokkok.modsys.namespace.Namespace
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.namespace.NamespaceResolver
import dalvik.system.DexClassLoader

object ModuleLoader {
    private const val TAG = "ModuleLoader"

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

    val loadedModules = HashMap<String, ModuleContainer>()

    fun loadModule(module: ModuleMetadata, errorCallback: (String) -> Unit, codeCacheDir: String) {
        if (module.id in loadedModules) return

        newModuleInstance(module, errorCallback, codeCacheDir)?.let {
            // Check if the requested namespace already exists
            if (NamespaceResolver.resolveNamespace("/${it.namespace}") != null) {
                // the same name already exists
                val err = "Failed to load module with the namespace ${it.namespace} since that namespace already exists"
                Log.w(TAG, err)
                errorCallback(err)

                return
            }

            loadedModules[module.name] = ModuleContainer(it)
        }
    }

    fun unloadAllModules() {
        for (moduleContainer in loadedModules.values) {
            moduleContainer.unload()
        }

        loadedModules.clear()
    }

    fun unloadModule(module: ModuleMetadata) = unloadModule(module.id)

    fun unloadModule(moduleId: String) {
        if (!loadedModules.containsKey(moduleId))
            throw IllegalArgumentException("The module id given isn't loaded")

        loadedModules[moduleId]!!.unload()
    }

    fun listLoadedModules(): List<String> = loadedModules.keys.toList()

    private fun newModuleInstance(module: ModuleMetadata, errorCallback: (String) -> Unit, codeCacheDir: String): Module? {
        val loader = DexClassLoader(module.jarPath, codeCacheDir, null, javaClass.classLoader)

        return try {
            val moduleClass = loader.loadClass(module.classpath)
            moduleClass.getConstructor().newInstance() as Module

        } catch (e: Error) {
            errorCallback("Error while loading module \"${module.name}\": ${e.message}")

            Log.e(TAG, "Error while loading module \"${module.name}\": ${e.message}")
            Log.e(TAG, "Full stacktrace: ", e)

            null
        }
    }
}