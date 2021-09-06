package com.blokkok.modsys.communication

import com.blokkok.modsys.communication.objects.Broadcaster
import com.blokkok.modsys.communication.objects.Subscription
import com.blokkok.modsys.communication.namespace.Namespace

/**
 * Communication API entry point
 */
@Suppress("unused") // because this is just a stub
class CommunicationContext(
    private val namespace: Namespace
) {
    /**
     * Creates a namespace with the [name] specified in the current namespace. Communications that's
     * defined inside the [block] will be added in a namespace with the specified [name]. It's also
     * allowed to nest namespaces.
     *
     * @param name The name used for the namespace
     * @param block The lambda where every communication defined there will be added into the
     *              namespace
     */
    fun namespace(name: String, block: CommunicationContext.() -> Unit) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Retrieves the [CommunicationType] of the communication name specified in the global
     * namespace. To get a communication type from a different namespace, check out
     * [getCommunication].
     *
     * @param name The communication name
     *
     * @see getCommunication
     *
     * @return The [CommunicationType] of this communication, will return null if the communication
     *          cannot be found
     */
    fun getCommunication(name: String): CommunicationType? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Retrieves the [CommunicationType] of the communication name specified in the [namespacePath]
     * parameter. If you ever need a shorthand for getting a communication on the global namespace,
     * check out [getCommunication]
     *
     * @param namespacePath The namespace where the communication is located
     * @param name The communication name
     *
     * @see getCommunication
     *
     * @return The [CommunicationType] of this communication, will return null if the communication
     *          cannot be found
     */
    fun getCommunication(namespacePath: String, name: String): CommunicationType? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Functions ===================================================================================

    /**
     * Creates a function in the current namespace with the [name] specified. [handler] will be the
     * function handler. Note that anyone can invoke this function from anywhere.
     *
     * @param name The name of the function
     * @param handler The handler for handling the function invocation
     *
     * @see invokeFunction
     */
    fun createFunction(name: String, handler: (Map<String, Any>) -> Any?) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Invokes a function with the specified [name] in the global namespace.
     *
     * @param name The name of the function inside the global namespace
     * @param args Arguments that will be passed onto the function
     *
     * @see invokeFunction
     * @see createFunction
     *
     * @return The return value of the function
     */
    fun invokeFunction(name: String, args: Map<String, Any> = emptyMap()): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Invokes a function with the specified [name] in the [namespace] specified.
     *
     * @param namespace The namespace where this function should be located
     * @param name The name of the function inside the global namespace
     * @param args Arguments that will be passed onto the function
     *
     * @see invokeFunction
     * @see createFunction
     *
     * @return The return value of the function
     */
    fun invokeFunction(namespace: String, name: String, args: Map<String, Any> = emptyMap()): Any? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Broadcast ===================================================================================

    /**
     * Creates a broadcaster where anyone can subscribe to in the current namespace with the
     * specified [name].
     *
     * @param name The name for the broadcaster
     *
     * @see subscribeToBroadcast
     */
    fun createBroadcaster(name: String): Broadcaster {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Subscribes to a broadcaster located in the global namespace with the [name] specified.
     *
     * @param name The name of the broadcaster in the global namespace
     * @param handler The handler when the broadcaster broadcasts a list of values
     *
     * @see subscribeToBroadcast
     * @see createBroadcaster
     *
     * @return The subscription of this broadcast, can be cancelled at any time
     */
    fun subscribeToBroadcast(name: String, handler: (List<Any?>) -> Unit): Subscription {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Subscribes to a broadcaster located in the [namespace] specified with the [name] specified.
     *
     * @param namespace The namespace of where the broadcast should be located
     * @param name The name of the broadcaster in the global namespace
     * @param handler The handler when the broadcaster broadcasts a list of values
     *
     * @see subscribeToBroadcast
     * @see createBroadcaster
     *
     * @return The subscription of this broadcast, can be cancelled at any time
     */
    fun subscribeToBroadcast(namespace: String, name: String, handler: (List<Any?>) -> Unit): Subscription {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Flags =======================================================================================

    /**
     * Declare that your module owns the [flagName] specified. A flag must only have one owner.
     * This function should be called before calling [getFlagNamespaces] or else it will throw an
     * exception.
     *
     * @param flagName The flag name your module wants to own
     */
    fun claimFlag(flagName: String) {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    /**
     * Retrieves the modules that has the flag name in their flag list. Make sure to call [claimFlag]
     * first before calling this, or else this will throw an exception.
     *
     * @param flagName The flag name you wanted to get every modules' namespace that has the flag name
     */
    fun getFlagNamespaces(flagName: String): List<String> {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }

    // Extension points ============================================================================

    fun <T> retrieveExtensions(clazz: Class<T>): ArrayList<T>? {
        throw RuntimeException("Stub! This jar is supposed to be compile only, do not use it as an implementation")
    }
}