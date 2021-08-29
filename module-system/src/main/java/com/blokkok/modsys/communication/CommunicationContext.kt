package com.blokkok.modsys.communication

import com.blokkok.modsys.ModuleFlagsManager
import com.blokkok.modsys.capitalizeCompat
import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Subscription
import com.blokkok.modsys.isCommunicationName
import com.blokkok.modsys.modinter.exception.AlreadyDefinedException
import com.blokkok.modsys.modinter.exception.IllegalFlagAccessException
import com.blokkok.modsys.modinter.exception.NotDefinedException
import com.blokkok.modsys.modinter.exception.TypeException
import com.blokkok.modsys.communication.namespace.Namespace
import com.blokkok.modsys.communication.namespace.NamespaceResolver
import com.blokkok.modsys.modinter.annotations.ExtensionPoint

/**
 * Communication API entry point
 */
@Suppress("unused")
class CommunicationContext(
    private val namespace: Namespace,
    private var ignoreAlreadyDefined: Boolean = false
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
        if (name in namespace.communications) {
            if (ignoreAlreadyDefined) return

            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")
        }

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
        if (name in namespace.communications) {
            if (ignoreAlreadyDefined)
                return (namespace.communications[name] as BroadcastCommunication).broadcaster

            throw AlreadyDefinedException("${namespace.communications[name]!!.name.capitalizeCompat()} $name is already defined in the current namespace")
        }

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
        // If the flag is already claimed, just do nothing
        val claimId = ModuleFlagsManager.claimFlag(flagName) ?: return

        claimedFlagIDs[flagName] = claimId
    }

    fun getFlagNamespaces(flagName: String): List<String> {
        if (flagName !in claimedFlagIDs)
            // this flag has not been claimed
            throw IllegalFlagAccessException(flagName)

        val modules = ModuleFlagsManager.getModulesWithFlag(flagName, claimedFlagIDs[flagName]!!)!!

        return modules.map { it.namespaceName }
    }

    // Extension point =============================================================================

    fun <T> retrieveExtensions(clazz: Class<T>): ArrayList<T>? {
        // check if clazz has the annotation
        val annotation = clazz.getAnnotation(ExtensionPoint::class.java) ?: return null

        // get the name of the extension point
        val name = if (annotation.name == "") clazz.simpleName else annotation.name

        // get and check if there is that communication extension on the current namespace
        val extPoint = namespace.communications[name] ?: return null
        if (extPoint !is ExtensionPointCommunication) return null

        @Suppress("UNCHECKED_CAST")
        // ^^^ extPoint.implementors is never going to be anything else, we've done the checks above
        return extPoint.implementors as ArrayList<T>
    }
}