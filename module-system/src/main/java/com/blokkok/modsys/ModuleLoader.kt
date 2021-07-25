package com.blokkok.modsys

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.blokkok.modsys.models.ModuleMetadata
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.communication.Broadcaster
import com.blokkok.modsys.modinter.communication.Stream
import com.blokkok.modsys.modinter.exception.NotDefinedException
import dalvik.system.DexClassLoader
import java.io.IOException
import java.lang.Exception

class ModuleLoader {

    @Suppress("PrivatePropertyName")
    private val TAG = "ModuleLoader"

    // These three variables stores every exported functions, streams, and broadcaster subscribers
    private val exportedFunctions      = HashMap<String, (Array<Any>) -> Any?           >()
    private val streams                = HashMap<String, Stream.() -> Unit              >()
    private val broadcasterSubscribers = HashMap<String, ArrayList<(Array<Any>) -> Unit>>()

    // These 4 variables is used to keep track of what stuff has been registered by a module
    // These hashmaps' keys are module IDs, and the value is a list of registered {something}
    private val registeredFunctions              = HashMap<String, ArrayList<String>>()
    private val registeredStreams                = HashMap<String, ArrayList<String>>()
    private val registeredBroadcasters           = HashMap<String, ArrayList<String>>()
    private val registeredBroadcasterSubscribers = HashMap<String, ArrayList<String>>()

    @Suppress("unused")
    inner class ModuleBridge {
        fun registerFunction(name: String, handler: (Array<Any>) -> Any?) {
            exportedFunctions[name] = handler
            registerFunction(name)
        }

        fun registerStream(name: String, streamHandler: Stream.() -> Unit) {
            streams[name] = streamHandler
            registerStream(name)
        }

        fun createBroadcaster(name: String): Broadcaster {
            registerBroadcaster(name)

            return object : Broadcaster() {
                override fun broadcast(vararg args: Any) {
                    broadcasterSubscribers[name]?.forEach { it(arrayOf(args)) }
                }
            }
        }

        fun invokeFunction(name: String, vararg args: Any) {
            if (!exportedFunctions.containsKey(name))
                throw NotDefinedException("exported function", name)

            exportedFunctions[name]!!.invoke(args.toList().toTypedArray())
        }

        fun openStream(name: String, streamHandler: Stream.() -> Unit) {
            if (!streams.containsKey(name))
                throw NotDefinedException("stream", name)

            val ingoingStream: Stream
            var outgoingStream: Stream? = null

            // Stream coming from the stream this module has opened into this module
            ingoingStream = object : Stream() {
                override fun send(value: Any) {
                    outgoingStream!!.ingoingBuffer.push(value)
                }

                @Throws(IOException::class)
                override fun receive(): Any {
                    // FIXME: 7/25/21 I don't think this is a good way of doing it
                    //  but I don't know any other way of doing it

                    do {
                        if (!ingoingBuffer.empty())
                            return ingoingBuffer.pop()

                        // Wait for the incoming stream to be sending a value
                        Thread.sleep(10)

                    } while (true)
                }

                override fun receiveCallback(callback: (Any) -> Unit) {
                    Thread { callback(receive()) }.run()
                }
            }

            // Stream coming from the this module into the stream this module has opened
            outgoingStream = object : Stream() {
                override fun send(value: Any) {
                    ingoingStream.ingoingBuffer.push(value)
                }

                @Throws(IOException::class)
                override fun receive(): Any {
                    // FIXME: 7/25/21 I don't think this is a good way of doing it
                    //  but I don't know any other way of doing it

                    do {
                        if (!ingoingBuffer.empty())
                            return ingoingBuffer.pop()

                        // Wait for the incoming stream to be sending a value
                        Thread.sleep(10)

                    } while (true)
                }

                override fun receiveCallback(callback: (Any) -> Unit) {
                    Thread { callback(receive()) }.run()
                }
            }

            Thread { streams[name]!!(outgoingStream) }.run()
            Thread { streamHandler(ingoingStream) }.run()
        }

        fun subscribeToBroadcast(name: String, handler: (Array<Any>) -> Unit) {
            if (!broadcasterSubscribers.containsKey(name))
                broadcasterSubscribers[name] = ArrayList()

            broadcasterSubscribers[name]!!.add(handler)

            registerBroadcastSubscriber(name)
        }

        fun unsubscribeToBroadcast(name: String) {
            broadcasterSubscribers.remove(name)
        }

        // =========================================================================================
        // Functions that are used to keep track of stuff

        private fun registerFunction(name: String) {
            if (!registeredFunctions.containsKey(name))
                registeredFunctions[name] = ArrayList()

            registeredFunctions[name]!!.add(name)
        }

        private fun registerStream(name: String) {
            if (!registeredStreams.containsKey(name))
                registeredStreams[name] = ArrayList()

            registeredStreams[name]!!.add(name)
        }

        private fun registerBroadcaster(name: String) {
            if (!registeredBroadcasters.containsKey(name))
                registeredBroadcasters[name] = ArrayList()

            registeredBroadcasters[name]!!.add(name)
        }

        private fun registerBroadcastSubscriber(name: String) {
            if (!registeredBroadcasterSubscribers.containsKey(name))
                registeredBroadcasterSubscribers[name] = ArrayList()

            registeredBroadcasterSubscribers[name]!!.add(name)
        }
    }

    private val loadedModules = HashMap<String, Module>()

    fun loadModule(module: ModuleMetadata, context: Context) {
        newModuleInstance(module, context)?.let {
            loadedModules[module.name] = it
            it.onLoad()
        }
    }

    fun unloadAllModules() {
        loadedModules.entries.forEach { (moduleId, moduleInst) ->
            moduleInst.onExit()
            clearCommunications(moduleId)
        }

        loadedModules.clear()
    }

    fun unloadModule(module: ModuleMetadata) = unloadModule(module.id)

    fun unloadModule(moduleId: String) {
        if (!loadedModules.containsKey(moduleId))
            throw IllegalArgumentException("The module id given isn't loaded")

        loadedModules[moduleId]!!.onExit()
        clearCommunications(moduleId)
    }

    fun isLoaded(moduleId: String) = loadedModules.containsKey(moduleId)

    // This function clears the communication of a module
    private fun clearCommunications(moduleId: String) {
        registeredFunctions[moduleId]?.let {
            it.forEach { funcName -> exportedFunctions.remove(funcName) }
            registeredFunctions.remove(moduleId)
        }

        registeredStreams[moduleId]?.let {
            it.forEach { funcName -> streams.remove(funcName) }
            registeredStreams.remove(moduleId)
        }

        registeredBroadcasters[moduleId]?.let {
            registeredBroadcasters.remove(moduleId)
        }

        registeredBroadcasterSubscribers[moduleId]?.let {
            it.forEach { funcName -> broadcasterSubscribers.remove(funcName) }
            registeredBroadcasterSubscribers.remove(moduleId)
        }
    }

    private fun newModuleInstance(module: ModuleMetadata, context: Context): Module? {
        val loader = DexClassLoader(module.jarPath, context.codeCacheDir.absolutePath, null, javaClass.classLoader)

        try {
            val moduleClass = loader.loadClass(module.classpath)

            val bridge = ModuleBridge()

            val constructor = moduleClass.getConstructor(ModuleBridge::class.java)
            return constructor.newInstance(bridge) as Module

        } catch (e: Exception) {
            Toast
                .makeText(
                    context,
                    "Error while loading module \"${module.name}\": ${e.message}",
                    Toast.LENGTH_SHORT
                )
                .show()

            Log.e(TAG, "Error while loading module \"${module.name}\": ${e.message}")
            Log.e(TAG, "Full stacktrace: ")
            e.printStackTrace()

            return null
        }
    }

    inline fun registerCommunications(registerer: ModuleBridge.() -> Unit) {
        ModuleBridge().registerer()
    }
}