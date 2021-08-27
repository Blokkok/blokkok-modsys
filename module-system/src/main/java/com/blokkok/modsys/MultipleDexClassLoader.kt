package com.blokkok.modsys

import dalvik.system.DexClassLoader

/**
 * Basically a class to load multiple dex files
 */
class MultipleDexClassLoader(
    private val optimizedDirectory: String?,
    private val librarySearchPath: String?,
) {
    private val loader by lazy {
        DexClassLoader("", optimizedDirectory, librarySearchPath, javaClass.classLoader)
    }

    // we're calling an internal API for adding the dex path, might not be good, I'll try other methods
    private val addDexPath = DexClassLoader::class.java
        .getMethod("addDexPath", String::class.java)

    fun loadDex(dexPath: String): DexClassLoader {
        addDexPath.invoke(loader, dexPath)

        return loader
    }
}