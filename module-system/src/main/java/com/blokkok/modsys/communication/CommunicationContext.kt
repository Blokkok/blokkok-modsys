package com.blokkok.modsys.communication

import com.blokkok.modsys.capitalizeCompat
import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Stream
import com.blokkok.modsys.communication.objects.Subscription
import com.blokkok.modsys.isCommunicationName
import com.blokkok.modsys.modinter.exception.AlreadyDefinedException
import com.blokkok.modsys.modinter.exception.NotDefinedException
import com.blokkok.modsys.modinter.exception.TypeException
import com.blokkok.modsys.namespace.Namespace
import com.blokkok.modsys.namespace.NamespaceResolver

/**
 * Communication API entry point
 */
@Suppress("unused")
class CommunicationContext(
    private val namespace: Namespace
) {
    fun namespace(name: String, block: CommunicationContext.() -> Unit) {
        val builder = CommunicationContext(
            NamespaceResolver.newNamespace(
                "",
                Namespace(name, parent = namespace),
                namespace
            )
        )

        block.invoke(builder)
    }

    // Functions ===================================================================================

    fun createFunction(name: String, handler: (List<Any?>) -> Any?) {
        // Check if the function name given is not alphanumeric
        if (!name.isCommunicationName())
            throw IllegalArgumentException("Function name \"$name\" must be alphanumeric or -+_")

        // Check if a communication with the same name already exists in the current namespace
        if (name in namespace.communications)
            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")

        namespace.communications[name] = FunctionCommunication(handler)
    }

    fun invokeFunction(name: String, args: List<Any?> = emptyList()): Any? {
        // since this function omits the namespace path, it's trying to invoke a function within the global namespace
        return invokeFunction("/", name, args)
    }

    fun invokeFunction(namespace: String, name: String, args: List<Any?> = emptyList()): Any? {
        // Resolve the namespace where the function we want is living in
        val functionNamespace = NamespaceResolver.resolveNamespace(namespace)
            ?: throw NotDefinedException("Namespace with the path", namespace)

        // Check if that function exists in the namespace
        val function = functionNamespace.communications[name]
            ?: throw NotDefinedException("Function with the name", name)

        // Check the communication type
        if (function !is FunctionCommunication)
            throw TypeException("Trying to invoke a function named $name, but that communication is a ${function.name}")

        return function.handler.invoke(args)
    }

    // Broadcast ===================================================================================

    fun createBroadcaster(name: String): Broadcaster {
        if (name in namespace.communications)
            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")

        val broadcaster = object : Broadcaster() {
            override fun broadcast(vararg args: Any?) {
                val broadcastCommunication = (namespace.communications[name] as BroadcastCommunication)
                for (subscriber in broadcastCommunication.subscribers) {
                    subscriber.handler.invoke(args.toList())
                }
            }
        }

        namespace.communications[name] = BroadcastCommunication(broadcaster)

        return broadcaster
    }

    fun subscribeToBroadcast(name: String, handler: (List<Any?>) -> Unit): Subscription =
        // since this function omits the namespace path, it's trying to subscribe to a broadcast within the global namespace
        subscribeToBroadcast("/", name, handler)

    fun subscribeToBroadcast(namespace: String, name: String, handler: (List<Any?>) -> Unit): Subscription {
        // Resolve the namespace where the broadcast we want is living in
        val broadcastNamespace = NamespaceResolver.resolveNamespace(namespace)
            ?: throw NotDefinedException("Namespace with the path", namespace)

        // get the broadcast that we wanted
        val broadcastCom = broadcastNamespace.communications[name]
            ?: throw NotDefinedException("Broadcast with the name", name)

        // Check the communication type
        if (broadcastCom !is BroadcastCommunication)
            throw TypeException("Trying to subscribe to a broadcast named $name, but that communication is a ${broadcastCom.name}")

        val subscription = Subscription(handler, broadcastCom)

        broadcastCom.subscribers.add(subscription)

        return subscription
    }

    // Streams =====================================================================================

    fun createStream(name: String, streamHandler: Stream.() -> Unit) {
        // Check if the name is alphanumeric / -_+
        if (!name.isCommunicationName())
            throw IllegalArgumentException("Function name \"$name\" must be alphanumeric or -_+")

        // Check if the name already exists
        if (name in namespace.communications)
            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")

        // Alright, let's put the stream
        namespace.communications[name] = StreamCommunication(streamHandler)
    }

    fun openStream(name: String, streamHandler: Stream.() -> Unit) =
        openStream("/", name, streamHandler)

    fun openStream(namespace: String, name: String, streamHandler: Stream.() -> Unit) {
        // Resolve the namespace where the stream we want is living in
        val streamNamespace = NamespaceResolver.resolveNamespace(namespace)
            ?: throw NotDefinedException("Namespace with the path", namespace)

        // Check if that function exists in the namespace
        val stream = streamNamespace.communications[name]
            ?: throw NotDefinedException("Function with the name", name)

        // Check the communication type
        if (stream !is StreamCommunication)
            throw TypeException("Trying to open a stream named $name, but that communication is a ${stream.name}")

        val ingoingStream  = Stream()
        val outgoingStream = Stream()

        ingoingStream.connectTo(outgoingStream)
        outgoingStream.connectTo(ingoingStream)

        Thread {
            streamHandler.invoke(outgoingStream)
            outgoingStream.close()
        }.start()

        Thread {
            stream.entryHandler.invoke(ingoingStream)
            ingoingStream.close()
        }.start()
    }
}