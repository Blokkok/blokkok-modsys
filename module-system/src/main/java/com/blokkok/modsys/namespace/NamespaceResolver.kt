package com.blokkok.modsys.namespace

import java.util.*

object NamespaceResolver {
    val globalNamespace = Namespace("")

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

        parentNamespace.children.add(namespace)
        namespace.parent = parentNamespace

        return namespace
    }

    /**
     * This function traverses the [globalNamespace] variable and tries to find a matching namespace
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
            else relativeNamespace
                ?: throw NullPointerException(
                    "relativeNamespace is null when trying to resolve a relative namespace path"
                )

        // Check if the size is one, and if that one item is empty
        if (splitPath.size == 1)
            if (splitPath.peek()!!.isEmpty())
                return currentNamespace

        val firstElem = splitPath.pop()

        for (child in globalNamespace.children) {
            if (child.name == firstElem) {
                // check if we need to go deeper
                return if (splitPath.isEmpty()) {
                    // yay this is the namespace we're looking for
                    child

                } else {
                    // call this recursively
                    resolveNamespace(
                        splitPath.toList().joinToString("/"),
                        currentNamespace
                    )
                }
            }
        }

        return null
    }
}