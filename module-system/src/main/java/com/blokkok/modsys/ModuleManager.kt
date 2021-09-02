package com.blokkok.modsys

import android.content.Context
import android.util.Log
import com.blokkok.modsys.communication.CommunicationContext
import com.blokkok.modsys.exceptions.IncompatibleModSysVersion
import com.blokkok.modsys.exceptions.InvalidAssetsFolderLocation
import com.blokkok.modsys.exceptions.SameIDException
import com.blokkok.modsys.models.ModuleManifest
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
    private const val TAG = "ModuleManager"

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
        HashMap<String, ModuleMetadata>().apply {
            modulesDir.listFiles()!!.forEach { folder ->
                runCatching {
                    put(folder.name, Json.decodeFromString(File(folder, "meta.json").readText()))
                }.onFailure {
                    // TODO: 8/25/21 show some kind of error to the user somehow
                    Log.e(TAG, "listModules: Cannot read ${folder.name} module's manifest, it might be corrupted", it)
                }
            }
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
     *
     * @throws ModuleDependencyResolver.ModuleDependencyNotFoundException Will be thrown if a module cannot find it's needed dependency
     */
    @Throws(ModuleDependencyResolver.ModuleDependencyNotFoundException::class)
    fun loadModules(errorCallback: (String) -> Unit, codeCacheDir: String) {
        val enabledModules = listEnabledModules()

        // make sure to load them correctly with their dependencies
        val orderedModules = ModuleDependencyResolver(enabledModules)
            .orderModules()

        orderedModules.forEach {
            ModuleLoader.loadModule(it, errorCallback, codeCacheDir)
        }

        ModuleLoader.finishLoadModules()
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
    @Throws(
        IOException::class,
        SameIDException::class,
        IncompatibleModSysVersion::class,
        InvalidAssetsFolderLocation::class,
    )
    fun importModule(zipInputStream: ZipInputStream, ignoreLibraryVersion: Boolean = false) {
        val cacheModuleExtractDir = File(cacheDir, "module-extract")
        cacheModuleExtractDir.mkdirs()

        unpackZip(zipInputStream, cacheModuleExtractDir)

        val manifest = Json.decodeFromString<ModuleManifest>(
            File(cacheModuleExtractDir, "manifest.json").readText()
        )

        val metadata = ModuleManifestParser.parseManifest(
            manifest,
            ignoreLibraryVersion
        )

        val loadedModules = listModules()

        // Check if we already have a module with the ID in this manifest
        if (loadedModules.containsKey(metadata.id))
            // yes we do have a module with the same id here, throw an exception
            throw SameIDException(metadata.id, loadedModules[metadata.id]!!.name)

        // Start moving stuff
        val location = File(modulesDir, metadata.id)
        location.mkdirs()

        // Move the module jar
        val moduleJar = File(cacheModuleExtractDir, metadata.jarPath)
        val moduleJarLocation = File(location, moduleJar.name)
        moduleJar.renameTo(moduleJarLocation)

        // Extract assets if it exists
        if (manifest.assetsFolder != null) {
            val assetsFolder = File(manifest.assetsFolder)

            // check if it's not some malicious path ("../..", ".", "/", "..")
            // by checking if this contains inside the cacheModuleExtractDir
            if (assetsFolder.absoluteFile.startsWith(cacheModuleExtractDir))
            // nope, it's pointing somewhere suspicious
                throw InvalidAssetsFolderLocation(manifest.name)

            // ok move the assets folder
            assetsFolder.renameTo(File(location, assetsFolder.name))
        }

        // Then put that moved module jar file into the final meta
        val meta = metadata.copy(jarPath = moduleJarLocation.absolutePath)

        // Save it
        File(location, "meta.json").writeText(Json.encodeToString(meta))

        // Clean up our mess
        cacheModuleExtractDir.deleteRecursively()
    }

    /**
     * Gets the assets folder of the specified module id
     */
    fun getAssetsFolder(moduleId: String): File? =
        listModules()[moduleId]?.let { meta ->
            meta.assetsFolder?.let { assetsFolder ->
                modulesDir.resolve(meta.id).resolve(assetsFolder)
            }
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

    @Throws(IOException::class)
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