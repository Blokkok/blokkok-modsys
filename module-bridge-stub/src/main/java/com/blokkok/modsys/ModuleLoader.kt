package com.blokkok.modsys

import com.blokkok.modsys.modinter.communication.Stream
import com.blokkok.modsys.modinter.communication.Broadcaster

class ModuleLoader {
    inner class ModuleBridge {
        // Registerer ==================================================================================
        /**
         * Registers an exported function that can be accessed by other modules
         * by using the function [invokeFunction]
         */
        fun registerFunction(name: String, handler: (Array<Any>) -> Any?) {
            throw RuntimeException("Stub!")
        }

        /**
         * Creates a stream that can be accessed by other modules
         * by using the function [openStream]
         */
        fun registerStream(name: String, streamHandler: Stream.() -> Unit) {
            throw RuntimeException("Stub!")
        }

        /**
         * Creates a broadcaster that can be subscribed to by other modules
         */
        fun createBroadcaster(name: String): Broadcaster {
            throw RuntimeException("Stub!")
        }

        // Accessor ====================================================================================
        /**
         * Invokes a function that has been registered by a module
         */
        fun invokeFunction(name: String, vararg args: Any) {
            throw RuntimeException("Stub!")
        }

        /**
         * Opens a stream that has been registered by a module
         */
        fun openStream(name: String, streamHandler: Stream.() -> Unit) {
            throw RuntimeException("Stub!")
        }

        /**
         * Subscribes to a broadcast that has been registered by a module
         */
        fun subscribeToBroadcast(name: String, handler: (Array<Any>) -> Unit) {
            throw RuntimeException("Stub!")
        }

        /**
         * Unsubscribes from the broadcast this module has subscribed earlier
         */
        fun unsubscribeToBroadcast(name: String) {
            throw RuntimeException("Stub!")
        }
    }
}