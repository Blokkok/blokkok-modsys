package com.blokkok.modsys.communication.namespace

import java.util.*

object NamespaceResolver {
    val globalNamespace = Namespace("<GLOBAL>")

    /**
     * Creates a new namespace on the specified path with the specified name
     */
    fun newNamespace(
        path: String,
        namespace: Namespace,
        relativeNamespace: Namespace? = null
    ): Namespace {
        val parentNamespace = resolveNamespace(path, relativeNamespace)
            ?: throw NullPointerException("Cannot find namespace at $path")

        parentNamespace.communications[namespace.namespaceName] = namespace
        namespace.parent = parentNamespace

        return namespace
    }

    fun deleteNamespace(
        path: String,
        relativeNamespace: Namespace? = null
    ) {
       val namespace = resolveNamespace(path, relativeNamespace)
       namespace?.parent?.communications?.remove(namespace.namespaceName)
    }

    /**
     * This function traverses the [globalNamespace] variable and tries to find a matching namespace
     *
     * @param path Path of the namespace you want to resolve
     * @param relativeNamespace The relative namespace you want to resolve on, if this is null and
     *                          the path doesn't start with /, it will use the global namespace instead
     */
    fun resolveNamespace(
        path: String,
        relativeNamespace: Namespace? = null
    ): Namespace? {
        if (path == "/") return globalNamespace // plain path pointing to /

        val splitPath = ArrayDeque(path.split("/"))
        if (path.startsWith("/")) splitPath.removeFirst() // remove the empty string at the start

        if (splitPath.isEmpty()) return relativeNamespace // check if this is just pointing to the current namespace

        // trying to get what the current namespace is, if it starts with /, then our
        // current namespace will be at the root
        val currentNamespace =
            if (path.startsWith("/")) globalNamespace
            else relativeNamespace ?: globalNamespace

        // Check if the size is one, and if that one item is empty
        if (splitPath.size == 1)
            if (splitPath.peek()!!.isEmpty())
                return currentNamespace

        val firstElem = splitPath.pop()

        for (child in currentNamespace.communications.values) {
            if (child !is Namespace) continue

            if (child.namespaceName == firstElem) {
                // check if we need to go deeper
                return if (splitPath.isEmpty()) {
                    // yay this is the namespace we're looking for
                    child

                } else {
                    // call this recursively
                    resolveNamespace(
                        splitPath.toList().joinToString("/"),
                        child
                    )
                }
            }
        }

        return null
    }
}