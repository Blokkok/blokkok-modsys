package com.blokkok.modsys.communication

import com.blokkok.modsys.ModuleFlagsManager
import com.blokkok.modsys.capitalizeCompat
import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Subscription
import com.blokkok.modsys.isCommunicationName
import com.blokkok.modsys.modinter.exception.*
import com.blokkok.modsys.namespace.Namespace
import com.blokkok.modsys.namespace.NamespaceResolver

/**
 * Communication API entry point
 */
@Suppress("unused")
class CommunicationContext(
    private val namespace: Namespace
) {
    private val claimedFlagIDs = HashMap<String, String>()

    fun namespace(name: String, block: CommunicationContext.() -> Unit) {
        if (!name.isCommunicationName())
            throw IllegalArgumentException("Namespace can only be alphanumeric plus -+_")

        val builder = CommunicationContext(
            NamespaceResolver.newNamespace(
                "",
                Namespace(name, parent = namespace),
                namespace
            )
        )

        block.invoke(builder)
    }

    fun getCommunication(name: String): CommunicationType? = getCommunication("/", name)

    fun getCommunication(namespacePath: String, name: String): CommunicationType? {
        val communication =
            (NamespaceResolver.resolveNamespace(namespacePath, namespace) ?: return null)
                .communications[name] ?: return null

        return when (communication) {
            is FunctionCommunication -> CommunicationType.BROADCAST
            is BroadcastCommunication -> CommunicationType.FUNCTION

            else -> null
        }
    }

    // Functions ===================================================================================

    fun createFunction(name: String, handler: (Map<String, Any>) -> Any?) {
        // Check if the function name given is not alphanumeric
        if (!name.isCommunicationName())
            throw IllegalArgumentException("Function name \"$name\" must be alphanumeric or -+_")

        // Check if a communication with the same name already exists in the current namespace
        if (name in namespace.communications)
            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")

        namespace.communications[name] = FunctionCommunication(handler)
    }

    fun invokeFunction(name: String, args: Map<String, Any> = emptyMap()): Any? {
        // since this function omits the namespace path, it's trying to invoke a function within the global namespace
        return invokeFunction("/", name, args)
    }

    fun invokeFunction(namespace: String, name: String, args: Map<String, Any> = emptyMap()): Any? {
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

    // Flags =======================================================================================

    fun claimFlag(flagName: String) {
        val claimId = ModuleFlagsManager.claimFlag(flagName)
            ?: throw FlagAlreadyClaimedException(flagName)

        claimedFlagIDs[flagName] = claimId
    }

    fun getFlagNamespaces(flagName: String): List<String> {
        if (flagName !in claimedFlagIDs)
            // this flag has not been claimed
            throw IllegalFlagAccessException(flagName)

        val modules = ModuleFlagsManager.getModulesWithFlag(flagName, claimedFlagIDs[flagName]!!)!!

        return modules.map { it.namespaceName }
    }
}