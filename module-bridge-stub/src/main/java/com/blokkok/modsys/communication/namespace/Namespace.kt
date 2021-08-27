package com.blokkok.modsys.communication.namespace

import com.blokkok.modsys.communication.Communication

/**
 * Stores data about a namespace
 */
class Namespace(
    val namespaceName: String,
    val communications: HashMap<String, Communication> = HashMap(),
    var parent: Namespace? = null,
): Communication() {
    override val name: String
        get() = throw RuntimeException(
            "Stub! This jar is supposed to be compile only, do not use it as an implementation"
        )
}