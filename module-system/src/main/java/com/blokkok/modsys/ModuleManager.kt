package com.blokkok.modsys

import android.content.Context
import com.blokkok.modsys.communication.CommunicationContext
import com.blokkok.modsys.exceptions.IncompatibleModSysVersion
import com.blokkok.modsys.exceptions.SameIDException
import com.blokkok.modsys.models.ModuleMetadata
import com.blokkok.modsys.namespace.NamespaceResolver
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/* Structure of the modules directory would be something like this:
 * modules
 * L myModule
 *   L meta.json    -- metadata of this module
 *   L module.jar   -- the jarfile
 */

/* Structure of a module zip:
 *
 * myModule.zip
 * L manifest.json
 * L module.jar
 */

/* Notes:
 * - The module folder name in the modules directory will be different from the real module name
 *   The module folder name will be called as "module id" as they identify each and every modules.
 *
 * - module.jar will be a dex-ed jar. meta.json will contain a classpath that points to a class
 *   inside the module.jar that will be the entry point of the module (the class must extend the
 *   module interface)
 */

@Suppress("unused", "MemberVisibilityCanBePrivate")
object ModuleManager {

    private lateinit var dataDir: File
    private lateinit var modulesDir: File

    private lateinit var cacheDir: File

    fun initialize(context: Context) {
        dataDir = File(context.applicationInfo.dataDir)
        modulesDir = File(dataDir, "modules")
        cacheDir = context.cacheDir

        if (!modulesDir.exists()) {
            modulesDir.mkdir()
        }
    }

    /**
     * Returns a list of modules corresponding with it's identifier
     */
    fun listModules(): Map<String, ModuleMetadata> =
        modulesDir.listFiles()!!.associate {
            it.name to Json.decodeFromString(File(it, "meta.json").readText())
        }

    /**
     * Gets the module metadata from the specified id
     */
    fun getModule(id: String): ModuleMetadata =
        Json.decodeFromString(
            File(modulesDir, id)
                .resolve("meta.json")
                .readText()
        )

    /**
     * Enables the module given, will be loaded when the function [loadModules] is invoked
     */
    fun enableModule(id: String) {
        toggleEnable(id, true)
    }

    /**
     * Disables the module given, will not be loaded when the function [loadModules] is invoked
     */
    fun disableModule(id: String) {
        toggleEnable(id, false)
    }

    /**
     * Loads all enabled modules
     */
    fun loadModules(errorCallback: (String) -> Unit, codeCacheDir: String) {
        val enabledModules = listEnabledModules()

        enabledModules.forEach {
            ModuleLoader.loadModule(it, errorCallback, codeCacheDir)
        }
    }

    /**
     * Unloads all modules
     */
    fun unloadModules() {
        ModuleLoader.unloadAllModules()
    }

    /**
     * Returns a list of module ids that are loaded
     */
    fun listLoadedModules(): List<String> = ModuleLoader.listLoadedModules()

    /**
     * Imports a module from a zip input stream of a module zip file
     */
    @Throws(SameIDException::class, IncompatibleModSysVersion::class)
    fun importModule(zipInputStream: ZipInputStream, ignoreLibraryVersion: Boolean = false) {
        val cacheModuleExtractDir = File(cacheDir, "module-extract")
        cacheModuleExtractDir.mkdirs()

        unpackZip(zipInputStream, cacheModuleExtractDir)

        val parsedManifest = ModuleManifestParser.parseManifest(
            File(cacheModuleExtractDir, "manifest.json").readText(),
            ignoreLibraryVersion
        )

        val loadedModules = listModules()

        // Check if we already have a module with the ID in this manifest
        if (loadedModules.containsKey(parsedManifest.id))
            // yes we do have a module with the same id here, throw an exception
            throw SameIDException(parsedManifest.id, loadedModules[parsedManifest.id]!!.name)

        // Start moving stuff
        val location = File(modulesDir, parsedManifest.id)
        location.mkdirs()

        // Move the module jar
        val moduleJar = File(cacheModuleExtractDir, parsedManifest.jarPath)
        val moduleJarLocation = File(location, moduleJar.name)
        moduleJar.renameTo(moduleJarLocation)

        // Then put that moved module jar file into the final meta
        val meta = parsedManifest.copy(jarPath = moduleJarLocation.absolutePath)

        // Save it
        File(location, "meta.json").writeText(Json.encodeToString(meta))

        // Clean up our mess
        cacheModuleExtractDir.deleteRecursively()
    }

    /**
     * Unloads the module specified (if it's loaded) then deletes it
     */
    fun deleteModule(id: String) {
        if (id in ModuleLoader.listLoadedModules()) {
            // this module is loaded, unload it
            ModuleLoader.unloadModule(id)
        }

        // then just delete the module
        File(modulesDir, id).deleteRecursively()
    }

    private val appCommunicationContext = CommunicationContext(
        NamespaceResolver.globalNamespace,
        ignoreAlreadyDefined = true // because an activity / fragment initialization can be called multiple times
    )

    /**
     * Execute communications, like creating a function on the global scope, or invoking a function
     * defined by a module
     */
    fun executeCommunications(block: CommunicationContext.() -> Unit) {
        block.invoke(appCommunicationContext)
    }

    private fun toggleEnable(
        id: String,
        enabled: Boolean,
    ) {
        val metaFile = File(modulesDir, id).resolve("meta.json")
        val meta = Json.decodeFromString<ModuleMetadata>(metaFile.readText())

        metaFile.writeText(
            Json.encodeToString(
                meta.copy(enabled = enabled)
            )
        )
    }

    private fun listEnabledModules(): List<ModuleMetadata> =
        listModules().values.filter { it.enabled }

    private fun unpackZip(
        zipInputStream: ZipInputStream,
        outputPath: File,
    ): Boolean {
        try {
            var filename: String

            var entry: ZipEntry?
            val buffer = ByteArray(1024)
            var count: Int

            while (zipInputStream.nextEntry.also { entry = it } != null) {
                filename = entry!!.name

                if (entry!!.isDirectory) {
                    File(outputPath, filename).mkdirs()
                    continue
                }

                val fileOut = FileOutputStream(File(outputPath, filename))

                while (zipInputStream.read(buffer).also { count = it } != -1)
                    fileOut.write(buffer, 0, count)

                fileOut.close()
                zipInputStream.closeEntry()
            }

            zipInputStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }
}