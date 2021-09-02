package com.blokkok.modsys

import android.util.Log
import com.blokkok.modsys.communication.namespace.NamespaceResolver
import com.blokkok.modsys.models.ModuleMetadata
import com.blokkok.modsys.modinter.Module
import dalvik.system.DexClassLoader

object ModuleLoader {
    private const val TAG = "ModuleLoader"

    private val loadedModules = HashMap<String, ModuleContainer>()
    private val loadedModulesMetadata = HashMap<String, ModuleMetadata>()

    fun loadModule(module: ModuleMetadata, errorCallback: (String) -> Unit, codeCacheDir: String) {
        if (module.id in loadedModules) return

        newModuleInstance(module, errorCallback, codeCacheDir)?.let {
            // Check if the requested namespace already exists
            if (NamespaceResolver.resolveNamespace("/${it.namespace}") != null) {
                // the same name already exists
                val err =
                    "Failed to load module with the namespace ${it.namespace} since that namespace already exists"
                Log.w(TAG, err)
                errorCallback(err)

                return
            }

            loadedModules[module.name] = ModuleContainer(it, module)
            loadedModulesMetadata[module.name] = module
        }
    }

    fun unloadAllModules() {
        for (moduleContainer in loadedModules.values) {
            moduleContainer.unload()
        }

        loadedModules.clear()
        loadedModulesMetadata.clear()
    }

    fun unloadModule(module: ModuleMetadata) = unloadModule(module.id)

    fun unloadModule(moduleId: String) {
        if (!loadedModules.containsKey(moduleId))
            throw IllegalArgumentException("The module id given isn't loaded")

        val module = loadedModules[moduleId]!!
        NamespaceResolver.deleteNamespace(module.namespaceName)
        module.unload()
        loadedModules.remove(moduleId)
        loadedModulesMetadata.remove(moduleId)
    }

    /**
     * This function is called when the [ModuleManager] finished loading all the modules
     *
     * This function is used to call onAllLoaded for each and every loaded modules
     */
    fun finishLoadModules() {
        for (value in loadedModules.values) {
            value.callOnAllLoaded()
        }
    }

    fun listLoadedModules(): List<String> = loadedModules.keys.toList()
    fun listLoadedModulesMetadata(): List<ModuleMetadata> = loadedModulesMetadata.values.toList()

    private var multipleDexClassLoader: MultipleDexClassLoader? = null

    private fun newModuleInstance(
        module: ModuleMetadata,
        errorCallback: (String) -> Unit,
        codeCacheDir: String
    ): Module? {
        if (multipleDexClassLoader == null) {
            multipleDexClassLoader = MultipleDexClassLoader(codeCacheDir, null)
        }

        val loader = multipleDexClassLoader!!.loadDex(module.jarPath)

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