package com.blokkok.modsys

import android.content.Context
import com.blokkok.modsys.models.ModuleMetadata
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
    private lateinit var loader: ModuleLoader

    fun initialize(context: Context) {
        dataDir = File(context.applicationInfo.dataDir)
        modulesDir = File(dataDir, "modules")
        cacheDir = context.cacheDir
        loader = ModuleLoader()

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

    /**
     * Loads all enabled modules
     */
    fun loadModules(errorCallback: (String) -> Unit, codeCacheDir: String) {
        val enabledModules = listEnabledModules()

        enabledModules.forEach {
            loader.loadModule(it, errorCallback, codeCacheDir)
        }
    }

    /**
     * Unloads all modules
     */
    fun unloadModules() {
        loader.unloadAllModules()
    }

    /**
     * Imports a module from a zip input stream of a module zip file
     */
    fun importModule(zipInputStream: ZipInputStream) {
        val cacheModuleExtractDir = File(cacheDir, "module-extract")
        cacheModuleExtractDir.mkdirs()

        unpackZip(zipInputStream, cacheModuleExtractDir)

        val parsedManifest = ModuleManifestParser.parseManifest(
            File(cacheModuleExtractDir, "manifest.json").readText()
        )

        val location = File(modulesDir, parsedManifest.id)
        location.mkdirs()

        val moduleJar = File(cacheModuleExtractDir, parsedManifest.jarPath)
        val moduleJarLocation = File(location, moduleJar.name)
        moduleJar.renameTo(moduleJarLocation)

        val meta = parsedManifest.copy(jarPath = moduleJarLocation.absolutePath)

        File(location, "meta.json").writeText(Json.encodeToString(meta))

        // clean up our mess
        cacheModuleExtractDir.deleteRecursively()
    }

    /**
     * Registers communications so other modules can communicate with the app
     */
    fun registerCommunications(registerer: ModuleLoader.ModuleBridge.() -> Unit) {
        loader.registerCommunications(registerer)
    }

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