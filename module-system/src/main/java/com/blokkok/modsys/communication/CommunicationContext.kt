package com.blokkok.modsys.communication

import com.blokkok.modsys.isCommunicationName
import com.blokkok.modsys.modinter.exception.AlreadyDefinedException
import com.blokkok.modsys.modinter.exception.NotDefinedException
import com.blokkok.modsys.modinter.exception.TypeException
import com.blokkok.modsys.namespace.Namespace
import com.blokkok.modsys.namespace.NamespaceResolver

/**
 * Communication API entry point
 */
class CommunicationContext(
    private val namespace: Namespace
) {
    fun createFunction(name: String, handler: (List<Any?>) -> Any?) {
        // Check if the function name given is not alphanumeric
        if (!name.isCommunicationName())
            throw IllegalArgumentException("Function name \"$name\" must be alphanumeric")

        // Check if a communication with the same name already exists in the current namespace
        if (name in namespace.communications)
            throw AlreadyDefinedException("Communication with name $name")

        namespace.communications[name] = FunctionCommunication(handler)
    }

    fun invokeFunction(name: String, args: List<Any?> = emptyList()): Any? {
        // since this function omits the namespace path, it's trying to invoke a function within the global namespace
        val function = NamespaceResolver.globalNamespace.communications[name]
            ?: throw NotDefinedException("Function with the name", name)

        // Check the communication type
        if (function !is FunctionCommunication)
            throw TypeException("Trying to invoke a function named $name, but that communication is a ${function.name}")

        return function.handler.invoke(args)
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
}